#!/bin/bash
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="${SCRIPT_DIR}/backend"
FRONTEND_DIR="${SCRIPT_DIR}/frontend"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[1;34m'
NC='\033[0m'

ACTION_STOP=false
ACTION_COMPILE=false
ACTION_START=false
ACTION_STATUS=false

TARGET_BACKEND=true
TARGET_FRONTEND=true

log_info() {
    echo -e "${GREEN}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" >&2
}

print_usage() {
    cat << EOF
用法: $(basename "$0") [选项]

Solo Video Platform 一键管理脚本

选项:
    动作选项（可组合使用）:
        --stop      停止服务
        --compile   编译项目
        --start     启动服务
        --status    查看服务状态（默认行为，不指定其他动作时）
    
    目标选项（不指定则同时操作前后端）:
        --backend   仅操作后端服务
        --frontend  仅操作前端服务
    
    其他:
        -h, --help  显示此帮助信息

默认行为（无参数）:
    查看服务状态（等价于 --status）

完整流程示例:
    # 停止 -> 编译 -> 启动 所有服务
    $(basename "$0") --stop --compile --start
    
    # 仅停止所有服务
    $(basename "$0") --stop
    
    # 仅编译后端
    $(basename "$0") --compile --backend
    
    # 启动前端服务
    $(basename "$0") --start --frontend
    
    # 查看后端状态
    $(basename "$0") --status --backend
    
    # 停止后端，启动前端
    $(basename "$0") --stop --backend --start --frontend
EOF
}

parse_args() {
    local has_action=false
    
    while [ $# -gt 0 ]; do
        case "$1" in
            --stop)
                ACTION_STOP=true
                has_action=true
                shift
                ;;
            --compile)
                ACTION_COMPILE=true
                has_action=true
                shift
                ;;
            --start)
                ACTION_START=true
                has_action=true
                shift
                ;;
            --status)
                ACTION_STATUS=true
                has_action=true
                shift
                ;;
            --backend)
                TARGET_BACKEND=true
                TARGET_FRONTEND=false
                shift
                ;;
            --frontend)
                TARGET_BACKEND=false
                TARGET_FRONTEND=true
                shift
                ;;
            -h|--help)
                print_usage
                exit 0
                ;;
            *)
                log_error "未知参数: $1"
                print_usage
                exit 1
                ;;
        esac
    done
    
    if [ "${has_action}" = false ]; then
        ACTION_STATUS=true
    fi
}

get_backend_pid() {
    if [ -f "${BACKEND_DIR}/.backend.pid" ]; then
        local pid
        pid=$(cat "${BACKEND_DIR}/.backend.pid" 2>/dev/null || true)
        if [ -n "${pid}" ] && kill -0 "${pid}" 2>/dev/null; then
            echo "${pid}"
            return 0
        fi
    fi
    
    local pid_from_port
    pid_from_port=$(lsof -t -iTCP:8080 -sTCP:LISTEN 2>/dev/null | head -1 || true)
    if [ -n "${pid_from_port}" ]; then
        echo "${pid_from_port}"
        return 0
    fi
    
    echo ""
    return 1
}

get_frontend_pid() {
    if [ -f "${FRONTEND_DIR}/.frontend.pid" ]; then
        local pid
        pid=$(cat "${FRONTEND_DIR}/.frontend.pid" 2>/dev/null || true)
        if [ -n "${pid}" ] && kill -0 "${pid}" 2>/dev/null; then
            echo "${pid}"
            return 0
        fi
    fi
    
    local pid_from_port
    pid_from_port=$(lsof -t -iTCP:5173 -sTCP:LISTEN 2>/dev/null | head -1 || true)
    if [ -n "${pid_from_port}" ]; then
        echo "${pid_from_port}"
        return 0
    fi
    
    echo ""
    return 1
}

is_backend_running() {
    lsof -n -P -iTCP:8080 -sTCP:LISTEN >/dev/null 2>&1
}

is_frontend_running() {
    lsof -n -P -iTCP:5173 -sTCP:LISTEN >/dev/null 2>&1
}

wait_for_port() {
    local port=$1
    local service_name=$2
    local max_wait=$3
    
    log_info "等待 ${service_name} 启动 (端口 ${port})..."
    
    local wait_count=0
    while [ ${wait_count} -lt ${max_wait} ]; do
        if lsof -n -P -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1; then
            log_info "${service_name} 启动成功 (端口 ${port})"
            return 0
        fi
        sleep 1
        wait_count=$((wait_count + 1))
        if [ $((wait_count % 5)) -eq 0 ]; then
            log_info "  已等待 ${wait_count} 秒..."
        fi
    done
    
    log_error "${service_name} 启动超时 (等待 ${max_wait} 秒)"
    return 1
}

stop_backend() {
    log_info "=== 停止后端服务 ==="
    
    local stopped=false
    
    local pid
    pid=$(get_backend_pid)
    if [ -n "${pid}" ]; then
        log_info "终止后端服务 (PID: ${pid})..."
        kill -9 "${pid}" 2>/dev/null || true
        rm -f "${BACKEND_DIR}/.backend.pid" 2>/dev/null
        stopped=true
    fi
    
    local maven_pids
    maven_pids=$(pgrep -f "mvn spring-boot:run" 2>/dev/null || true)
    if [ -n "${maven_pids}" ]; then
        log_warn "发现残留的 Maven 进程: ${maven_pids}"
        for mp in ${maven_pids}; do
            if kill -0 "${mp}" 2>/dev/null; then
                log_info "  终止进程 PID: ${mp}"
                kill -9 "${mp}" 2>/dev/null || true
                stopped=true
            fi
        done
    fi
    
    if [ "${stopped}" = true ]; then
        local max_wait=15
        local wait_count=0
        while [ ${wait_count} -lt ${max_wait} ]; do
            if ! is_backend_running; then
                break
            fi
            sleep 1
            wait_count=$((wait_count + 1))
        done
    fi
    
    if is_backend_running; then
        log_warn "后端服务可能仍在运行"
    else
        log_info "后端服务已停止"
    fi
    echo ""
}

stop_frontend() {
    log_info "=== 停止前端服务 ==="
    
    local stopped=false
    
    local pid
    pid=$(get_frontend_pid)
    if [ -n "${pid}" ]; then
        log_info "终止前端服务 (PID: ${pid})..."
        kill -9 "${pid}" 2>/dev/null || true
        rm -f "${FRONTEND_DIR}/.frontend.pid" 2>/dev/null
        stopped=true
    fi
    
    local vite_pids
    vite_pids=$(pgrep -f "vite" 2>/dev/null || true)
    if [ -n "${vite_pids}" ]; then
        log_warn "发现残留的 Vite 进程: ${vite_pids}"
        for vp in ${vite_pids}; do
            if kill -0 "${vp}" 2>/dev/null; then
                log_info "  终止进程 PID: ${vp}"
                kill -9 "${vp}" 2>/dev/null || true
                stopped=true
            fi
        done
    fi
    
    if [ "${stopped}" = true ]; then
        local max_wait=15
        local wait_count=0
        while [ ${wait_count} -lt ${max_wait} ]; do
            if ! is_frontend_running; then
                break
            fi
            sleep 1
            wait_count=$((wait_count + 1))
        done
    fi
    
    if is_frontend_running; then
        log_warn "前端服务可能仍在运行"
    else
        log_info "前端服务已停止"
    fi
    echo ""
}

stop_services() {
    log_info "=========================================="
    log_info "停止服务"
    log_info "=========================================="
    echo ""
    
    if [ "${TARGET_BACKEND}" = true ]; then
        stop_backend
    fi
    
    if [ "${TARGET_FRONTEND}" = true ]; then
        stop_frontend
    fi
    
    log_info "停止完成"
    echo ""
}

compile_backend() {
    log_info "=== 编译后端项目 ==="
    cd "${BACKEND_DIR}"
    
    log_info "执行: mvn clean install -DskipTests"
    if ! mvn clean install -DskipTests -q; then
        log_error "后端编译失败"
        exit 1
    fi
    
    log_info "后端编译成功"
    echo ""
}

compile_frontend() {
    log_info "=== 编译前端项目 ==="
    cd "${FRONTEND_DIR}"
    
    log_info "执行: npm run build"
    if ! npm run build; then
        log_error "前端编译失败"
        exit 1
    fi
    
    log_info "前端编译成功"
    echo ""
}

compile_projects() {
    log_info "=========================================="
    log_info "编译项目"
    log_info "=========================================="
    echo ""
    
    if [ "${TARGET_BACKEND}" = true ]; then
        compile_backend
    fi
    
    if [ "${TARGET_FRONTEND}" = true ]; then
        compile_frontend
    fi
    
    log_info "编译完成"
    echo ""
}

create_log_dirs() {
    mkdir -p "${BACKEND_DIR}/logs"
    mkdir -p "${FRONTEND_DIR}/logs"
}

start_backend() {
    log_info "=== 启动后端服务 ==="
    
    if is_backend_running; then
        local pid
        pid=$(get_backend_pid)
        log_warn "后端服务已在运行 (PID: ${pid})"
        echo ""
        return
    fi
    
    create_log_dirs
    cd "${BACKEND_DIR}"
    
    log_info "执行: mvn spring-boot:run (后台运行)"
    nohup mvn spring-boot:run > "${BACKEND_DIR}/logs/server.log" 2>&1 &
    local backend_pid=$!
    
    log_info "后端服务正在启动，PID: ${backend_pid}"
    log_info "日志文件: ${BACKEND_DIR}/logs/server.log"
    
    if wait_for_port 8080 "后端服务" 60; then
        echo "${backend_pid}" > "${BACKEND_DIR}/.backend.pid"
        log_info "后端 PID 已保存到: ${BACKEND_DIR}/.backend.pid"
    else
        log_error "后端服务启动失败"
        log_error "请检查日志: ${BACKEND_DIR}/logs/server.log"
        exit 1
    fi
    echo ""
}

start_frontend() {
    log_info "=== 启动前端服务 ==="
    
    if is_frontend_running; then
        local pid
        pid=$(get_frontend_pid)
        log_warn "前端服务已在运行 (PID: ${pid})"
        echo ""
        return
    fi
    
    create_log_dirs
    cd "${FRONTEND_DIR}"
    
    log_info "执行: npm run dev (后台运行)"
    nohup npm run dev > "${FRONTEND_DIR}/logs/server.log" 2>&1 &
    local frontend_pid=$!
    
    log_info "前端服务正在启动，PID: ${frontend_pid}"
    log_info "日志文件: ${FRONTEND_DIR}/logs/server.log"
    
    if wait_for_port 5173 "前端服务" 30; then
        echo "${frontend_pid}" > "${FRONTEND_DIR}/.frontend.pid"
        log_info "前端 PID 已保存到: ${FRONTEND_DIR}/.frontend.pid"
    else
        log_error "前端服务启动失败"
        log_error "请检查日志: ${FRONTEND_DIR}/logs/server.log"
        exit 1
    fi
    echo ""
}

start_services() {
    log_info "=========================================="
    log_info "启动服务"
    log_info "=========================================="
    echo ""
    
    if [ "${TARGET_BACKEND}" = true ]; then
        start_backend
    fi
    
    if [ "${TARGET_FRONTEND}" = true ]; then
        start_frontend
    fi
    
    log_info "启动完成"
    echo ""
}

print_status() {
    log_info "=========================================="
    log_info "服务状态"
    log_info "=========================================="
    echo ""
    
    if [ "${TARGET_BACKEND}" = true ]; then
        log_info "${BLUE}[后端服务]${NC}"
        if is_backend_running; then
            local pid
            pid=$(get_backend_pid)
            log_info "  状态: ${GREEN}运行中${NC}"
            log_info "  PID:  ${pid}"
            log_info "  端口: 8080"
            log_info "  地址: http://localhost:8080/"
            log_info "  H2控制台: http://localhost:8080/h2-console"
        else
            log_info "  状态: ${RED}未运行${NC}"
            log_info "  端口: 8080 (未监听)"
        fi
        echo ""
    fi
    
    if [ "${TARGET_FRONTEND}" = true ]; then
        log_info "${BLUE}[前端服务]${NC}"
        if is_frontend_running; then
            local pid
            pid=$(get_frontend_pid)
            log_info "  状态: ${GREEN}运行中${NC}"
            log_info "  PID:  ${pid}"
            log_info "  端口: 5173"
            log_info "  地址: http://localhost:5173/"
        else
            log_info "  状态: ${RED}未运行${NC}"
            log_info "  端口: 5173 (未监听)"
        fi
        echo ""
    fi
    
    if [ "${TARGET_BACKEND}" = true ] && [ "${TARGET_FRONTEND}" = true ]; then
        log_info "=========================================="
        log_info "快速操作提示"
        log_info "=========================================="
        echo ""
        
        if ! is_backend_running && ! is_frontend_running; then
            log_info "启动所有服务: $0 --start"
        elif is_backend_running || is_frontend_running; then
            log_info "停止所有服务: $0 --stop"
        fi
        log_info "完整重启: $0 --stop --compile --start"
        echo ""
    fi
}

print_summary() {
    echo ""
    log_info "=========================================="
    log_info "执行摘要"
    log_info "=========================================="
    echo ""
    
    local actions=()
    [ "${ACTION_STOP}" = true ] && actions+=("停止")
    [ "${ACTION_COMPILE}" = true ] && actions+=("编译")
    [ "${ACTION_START}" = true ] && actions+=("启动")
    [ "${ACTION_STATUS}" = true ] && actions+=("状态查询")
    
    local targets=()
    [ "${TARGET_BACKEND}" = true ] && targets+=("后端")
    [ "${TARGET_FRONTEND}" = true ] && targets+=("前端")
    
    log_info "执行动作: ${actions[*]}"
    log_info "操作目标: ${targets[*]}"
    echo ""
}

main() {
    parse_args "$@"
    
    log_info "=========================================="
    log_info "Solo Video Platform 一键管理脚本"
    log_info "=========================================="
    echo ""
    
    if [ "${ACTION_STOP}" = true ] || [ "${ACTION_COMPILE}" = true ] || [ "${ACTION_START}" = true ]; then
        print_summary
    fi
    
    if [ "${ACTION_STOP}" = true ]; then
        stop_services
    fi
    
    if [ "${ACTION_COMPILE}" = true ]; then
        compile_projects
    fi
    
    if [ "${ACTION_START}" = true ]; then
        start_services
    fi
    
    if [ "${ACTION_STATUS}" = true ]; then
        print_status
    fi
    
    if [ "${ACTION_START}" = true ]; then
        log_info "=========================================="
        log_info "服务地址"
        log_info "=========================================="
        echo ""
        if [ "${TARGET_BACKEND}" = true ] && is_backend_running; then
            log_info "  - 后端 API:  http://localhost:8080/"
            log_info "  - H2 控制台: http://localhost:8080/h2-console"
        fi
        if [ "${TARGET_FRONTEND}" = true ] && is_frontend_running; then
            log_info "  - 前端应用:  http://localhost:5173/"
        fi
        echo ""
    fi
}

main "$@"
