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

STOP_BACKEND=false
STOP_FRONTEND=false
COMPILE_BACKEND=false
COMPILE_FRONTEND=false
START_BACKEND=false
START_FRONTEND=false
STATUS_BACKEND=false
STATUS_FRONTEND=false

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

get_pid_by_port() {
    local port=$1
    lsof -t -iTCP:"${port}" -sTCP:LISTEN 2>/dev/null | head -1 || true
}

get_pid_by_file() {
    local pid_file=$1
    if [ -f "${pid_file}" ]; then
        local pid
        pid=$(cat "${pid_file}" 2>/dev/null || true)
        if [ -n "${pid}" ] && kill -0 "${pid}" 2>/dev/null; then
            echo "${pid}"
            return 0
        fi
    fi
    echo ""
    return 1
}

get_backend_pid() {
    local pid
    pid=$(get_pid_by_file "${BACKEND_DIR}/.backend.pid")
    if [ -n "${pid}" ]; then
        echo "${pid}"
        return 0
    fi
    
    pid=$(get_pid_by_port 8080)
    if [ -n "${pid}" ]; then
        echo "${pid}"
        return 0
    fi
    
    echo ""
    return 1
}

get_frontend_pid() {
    local pid
    pid=$(get_pid_by_file "${FRONTEND_DIR}/.frontend.pid")
    if [ -n "${pid}" ]; then
        echo "${pid}"
        return 0
    fi
    
    pid=$(get_pid_by_port 5173)
    if [ -n "${pid}" ]; then
        echo "${pid}"
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

kill_process() {
    local pid=$1
    local name=$2
    
    if [ -z "${pid}" ]; then
        return 1
    fi
    
    if ! kill -0 "${pid}" 2>/dev/null; then
        return 1
    fi
    
    log_info "  终止 ${name} (PID: ${pid})..."
    
    kill -TERM "${pid}" 2>/dev/null || true
    
    local max_wait=5
    local wait_count=0
    while kill -0 "${pid}" 2>/dev/null && [ ${wait_count} -lt ${max_wait} ]; do
        sleep 1
        ((wait_count++))
    done
    
    if kill -0 "${pid}" 2>/dev/null; then
        log_warn "  进程 ${pid} 未响应 SIGTERM，强制终止..."
        kill -9 "${pid}" 2>/dev/null || true
        sleep 1
    fi
    
    if kill -0 "${pid}" 2>/dev/null; then
        log_error "  无法终止进程 ${pid}"
        return 1
    fi
    
    return 0
}

kill_process_by_port() {
    local port=$1
    local name=$2
    
    local pids
    pids=$(lsof -t -iTCP:"${port}" -sTCP:LISTEN 2>/dev/null || true)
    
    if [ -z "${pids}" ]; then
        return 1
    fi
    
    for pid in ${pids}; do
        kill_process "${pid}" "${name}"
    done
    
    return 0
}

kill_all_maven_processes() {
    local pids
    pids=$(pgrep -f "mvn spring-boot:run" 2>/dev/null || true)
    
    if [ -z "${pids}" ]; then
        return 1
    fi
    
    log_warn "发现残留的 Maven 进程..."
    for pid in ${pids}; do
        kill_process "${pid}" "Maven"
    done
    
    return 0
}

kill_all_vite_processes() {
    local pids
    pids=$(pgrep -f "vite" 2>/dev/null || true)
    
    if [ -z "${pids}" ]; then
        return 1
    fi
    
    log_warn "发现残留的 Vite 进程..."
    for pid in ${pids}; do
        kill_process "${pid}" "Vite"
    done
    
    return 0
}

wait_for_port_down() {
    local port=$1
    local name=$2
    local max_wait=${3:-15}
    
    local wait_count=0
    while [ ${wait_count} -lt ${max_wait} ]; do
        if ! lsof -n -P -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1; then
            return 0
        fi
        sleep 1
        ((wait_count++))
    done
    
    if lsof -n -P -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1; then
        return 1
    fi
    
    return 0
}

wait_for_port_up() {
    local port=$1
    local name=$2
    local max_wait=$3
    
    local wait_count=0
    while [ ${wait_count} -lt ${max_wait} ]; do
        if lsof -n -P -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1; then
            return 0
        fi
        sleep 1
        ((wait_count++))
        if [ $((wait_count % 5)) -eq 0 ]; then
            log_info "  已等待 ${wait_count} 秒..."
        fi
    done
    
    if lsof -n -P -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1; then
        return 0
    fi
    
    return 1
}

stop_backend() {
    log_info "=========================================="
    log_info "停止后端服务"
    log_info "=========================================="
    echo ""
    
    local stopped=false
    
    if ! is_backend_running; then
        log_info "后端服务未运行"
        echo ""
        return
    fi
    
    local pid
    pid=$(get_backend_pid)
    if [ -n "${pid}" ]; then
        if kill_process "${pid}" "后端服务"; then
            rm -f "${BACKEND_DIR}/.backend.pid" 2>/dev/null
            stopped=true
        fi
    fi
    
    kill_all_maven_processes
    
    kill_process_by_port 8080 "后端服务（通过端口）"
    
    if [ "${stopped}" = true ] || is_backend_running; then
        log_info "等待后端服务停止..."
        if wait_for_port_down 8080 "后端服务"; then
            log_info "后端服务已停止"
        else
            log_warn "后端服务可能仍在运行（端口 8080 仍被占用）"
        fi
    else
        log_info "后端服务已停止"
    fi
    
    echo ""
}

stop_frontend() {
    log_info "=========================================="
    log_info "停止前端服务"
    log_info "=========================================="
    echo ""
    
    local stopped=false
    
    if ! is_frontend_running; then
        log_info "前端服务未运行"
        echo ""
        return
    fi
    
    local pid
    pid=$(get_frontend_pid)
    if [ -n "${pid}" ]; then
        if kill_process "${pid}" "前端服务"; then
            rm -f "${FRONTEND_DIR}/.frontend.pid" 2>/dev/null
            stopped=true
        fi
    fi
    
    kill_all_vite_processes
    
    kill_process_by_port 5173 "前端服务（通过端口）"
    
    if [ "${stopped}" = true ] || is_frontend_running; then
        log_info "等待前端服务停止..."
        if wait_for_port_down 5173 "前端服务"; then
            log_info "前端服务已停止"
        else
            log_warn "前端服务可能仍在运行（端口 5173 仍被占用）"
        fi
    else
        log_info "前端服务已停止"
    fi
    
    echo ""
}

compile_backend() {
    log_info "=========================================="
    log_info "编译后端项目"
    log_info "=========================================="
    echo ""
    
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
    log_info "=========================================="
    log_info "编译前端项目"
    log_info "=========================================="
    echo ""
    
    cd "${FRONTEND_DIR}"
    
    log_info "执行: npm run build"
    if ! npm run build; then
        log_error "前端编译失败"
        exit 1
    fi
    
    log_info "前端编译成功"
    echo ""
}

create_log_dirs() {
    mkdir -p "${BACKEND_DIR}/logs"
    mkdir -p "${FRONTEND_DIR}/logs"
}

start_backend() {
    log_info "=========================================="
    log_info "启动后端服务"
    log_info "=========================================="
    echo ""
    
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
    log_info "等待后端服务启动（最多 60 秒）..."
    
    if wait_for_port_up 8080 "后端服务" 60; then
        echo "${backend_pid}" > "${BACKEND_DIR}/.backend.pid"
        log_info "后端服务启动成功"
        log_info "后端 PID 已保存到: ${BACKEND_DIR}/.backend.pid"
    else
        log_error "后端服务启动超时（等待 60 秒）"
        log_error "请检查日志: ${BACKEND_DIR}/logs/server.log"
        exit 1
    fi
    
    echo ""
}

start_frontend() {
    log_info "=========================================="
    log_info "启动前端服务"
    log_info "=========================================="
    echo ""
    
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
    log_info "等待前端服务启动（最多 30 秒）..."
    
    if wait_for_port_up 5173 "前端服务" 30; then
        echo "${frontend_pid}" > "${FRONTEND_DIR}/.frontend.pid"
        log_info "前端服务启动成功"
        log_info "前端 PID 已保存到: ${FRONTEND_DIR}/.frontend.pid"
    else
        log_error "前端服务启动超时（等待 30 秒）"
        log_error "请检查日志: ${FRONTEND_DIR}/logs/server.log"
        exit 1
    fi
    
    echo ""
}

print_status_backend() {
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
}

print_status_frontend() {
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
}

print_status() {
    log_info "=========================================="
    log_info "服务状态"
    log_info "=========================================="
    echo ""
    
    if [ "${STATUS_BACKEND}" = true ]; then
        print_status_backend
    fi
    
    if [ "${STATUS_FRONTEND}" = true ]; then
        print_status_frontend
    fi
    
    if [ "${STATUS_BACKEND}" = true ] && [ "${STATUS_FRONTEND}" = true ]; then
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
    [ "${STOP_BACKEND}" = true ] || [ "${STOP_FRONTEND}" = true ] && actions+=("停止")
    [ "${COMPILE_BACKEND}" = true ] || [ "${COMPILE_FRONTEND}" = true ] && actions+=("编译")
    [ "${START_BACKEND}" = true ] || [ "${START_FRONTEND}" = true ] && actions+=("启动")
    [ "${STATUS_BACKEND}" = true ] || [ "${STATUS_FRONTEND}" = true ] && actions+=("状态查询")
    
    log_info "执行动作: ${actions[*]}"
    echo ""
}

print_service_addresses() {
    local has_backend=false
    local has_frontend=false
    
    if [ "${START_BACKEND}" = true ] && is_backend_running; then
        has_backend=true
    fi
    
    if [ "${START_FRONTEND}" = true ] && is_frontend_running; then
        has_frontend=true
    fi
    
    if [ "${has_backend}" = true ] || [ "${has_frontend}" = true ]; then
        log_info "=========================================="
        log_info "服务地址"
        log_info "=========================================="
        echo ""
        
        if [ "${has_backend}" = true ]; then
            log_info "  - 后端 API:  http://localhost:8080/"
            log_info "  - H2 控制台: http://localhost:8080/h2-console"
        fi
        
        if [ "${has_frontend}" = true ]; then
            log_info "  - 前端应用:  http://localhost:5173/"
        fi
        
        echo ""
    fi
}

parse_args() {
    local has_action=false
    local has_target=false
    
    local pending_stop=false
    local pending_compile=false
    local pending_start=false
    local pending_status=false
    
    while [ $# -gt 0 ]; do
        case "$1" in
            --stop)
                pending_stop=true
                has_action=true
                shift
                ;;
            --compile)
                pending_compile=true
                has_action=true
                shift
                ;;
            --start)
                pending_start=true
                has_action=true
                shift
                ;;
            --status)
                pending_status=true
                has_action=true
                shift
                ;;
            --backend)
                if [ "${pending_stop}" = true ]; then
                    STOP_BACKEND=true
                    pending_stop=false
                fi
                if [ "${pending_compile}" = true ]; then
                    COMPILE_BACKEND=true
                    pending_compile=false
                fi
                if [ "${pending_start}" = true ]; then
                    START_BACKEND=true
                    pending_start=false
                fi
                if [ "${pending_status}" = true ]; then
                    STATUS_BACKEND=true
                    pending_status=false
                fi
                has_target=true
                shift
                ;;
            --frontend)
                if [ "${pending_stop}" = true ]; then
                    STOP_FRONTEND=true
                    pending_stop=false
                fi
                if [ "${pending_compile}" = true ]; then
                    COMPILE_FRONTEND=true
                    pending_compile=false
                fi
                if [ "${pending_start}" = true ]; then
                    START_FRONTEND=true
                    pending_start=false
                fi
                if [ "${pending_status}" = true ]; then
                    STATUS_FRONTEND=true
                    pending_status=false
                fi
                has_target=true
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
        STATUS_BACKEND=true
        STATUS_FRONTEND=true
        return
    fi
    
    if [ "${has_target}" = false ]; then
        if [ "${pending_stop}" = true ]; then
            STOP_BACKEND=true
            STOP_FRONTEND=true
        fi
        if [ "${pending_compile}" = true ]; then
            COMPILE_BACKEND=true
            COMPILE_FRONTEND=true
        fi
        if [ "${pending_start}" = true ]; then
            START_BACKEND=true
            START_FRONTEND=true
        fi
        if [ "${pending_status}" = true ]; then
            STATUS_BACKEND=true
            STATUS_FRONTEND=true
        fi
    else
        if [ "${pending_stop}" = true ] || [ "${pending_compile}" = true ] || [ "${pending_start}" = true ] || [ "${pending_status}" = true ]; then
            log_warn "警告: 动作选项必须在目标选项之前"
            log_warn "例如: --stop --backend 而不是 --backend --stop"
        fi
    fi
}

main() {
    parse_args "$@"
    
    log_info "=========================================="
    log_info "Solo Video Platform 一键管理脚本"
    log_info "=========================================="
    echo ""
    
    if [ "${STOP_BACKEND}" = true ] || [ "${STOP_FRONTEND}" = true ] || \
       [ "${COMPILE_BACKEND}" = true ] || [ "${COMPILE_FRONTEND}" = true ] || \
       [ "${START_BACKEND}" = true ] || [ "${START_FRONTEND}" = true ]; then
        print_summary
    fi
    
    if [ "${STOP_BACKEND}" = true ]; then
        stop_backend
    fi
    
    if [ "${STOP_FRONTEND}" = true ]; then
        stop_frontend
    fi
    
    if [ "${COMPILE_BACKEND}" = true ]; then
        compile_backend
    fi
    
    if [ "${COMPILE_FRONTEND}" = true ]; then
        compile_frontend
    fi
    
    if [ "${START_BACKEND}" = true ]; then
        start_backend
    fi
    
    if [ "${START_FRONTEND}" = true ]; then
        start_frontend
    fi
    
    if [ "${STATUS_BACKEND}" = true ] || [ "${STATUS_FRONTEND}" = true ]; then
        print_status
    fi
    
    print_service_addresses
}

main "$@"
