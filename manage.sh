#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="${SCRIPT_DIR}/backend"
FRONTEND_DIR="${SCRIPT_DIR}/frontend"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ACTION_STOP=false
ACTION_COMPILE=false
ACTION_START=false
ACTION_ALL=false

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
    --stop      停止所有服务
    --compile   编译前后端项目
    --start     启动所有服务
    --all       执行停止 -> 编译 -> 启动（默认行为）
    -h, --help  显示此帮助信息

示例:
    $(basename "$0") --all          停止服务、编译、启动服务
    $(basename "$0") --stop         仅停止服务
    $(basename "$0") --compile      仅编译项目
    $(basename "$0") --start        仅启动服务
    $(basename "$0") --stop --start 停止后启动（不编译）
EOF
}

parse_args() {
    if [ $# -eq 0 ]; then
        ACTION_ALL=true
        return
    fi
    
    while [ $# -gt 0 ]; do
        case "$1" in
            --stop)
                ACTION_STOP=true
                shift
                ;;
            --compile)
                ACTION_COMPILE=true
                shift
                ;;
            --start)
                ACTION_START=true
                shift
                ;;
            --all)
                ACTION_ALL=true
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
    
    if [ "${ACTION_ALL}" = true ]; then
        ACTION_STOP=true
        ACTION_COMPILE=true
        ACTION_START=true
    fi
}

stop_by_pid_file() {
    local pid_file=$1
    local service_name=$2
    
    if [ -f "${pid_file}" ]; then
        local pid
        pid=$(cat "${pid_file}" 2>/dev/null || true)
        
        if [ -n "${pid}" ] && kill -0 "${pid}" 2>/dev/null; then
            log_info "终止 ${service_name} (PID: ${pid})..."
            kill -9 "${pid}" 2>/dev/null || true
            rm -f "${pid_file}" 2>/dev/null
            log_info "${service_name} 已终止"
            return 0
        else
            log_warn "${service_name} PID 文件存在但进程不存在，清理 PID 文件"
            rm -f "${pid_file}" 2>/dev/null
        fi
    fi
    return 1
}

stop_by_port() {
    local port=$1
    local service_name=$2
    
    if lsof -n -P -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1; then
        local pids
        pids=$(lsof -t -iTCP:"${port}" -sTCP:LISTEN 2>/dev/null || true)
        
        if [ -n "${pids}" ]; then
            log_info "终止端口 ${port} 上的 ${service_name}..."
            for pid in ${pids}; do
                if kill -0 "${pid}" 2>/dev/null; then
                    log_info "  终止进程 PID: ${pid}"
                    kill -9 "${pid}" 2>/dev/null || true
                fi
            done
            
            local max_wait=10
            local wait_count=0
            while lsof -n -P -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1 && [ ${wait_count} -lt ${max_wait} ]; do
                sleep 1
                ((wait_count++))
            done
            
            if lsof -n -P -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1; then
                log_warn "可能有残留进程在端口 ${port} 上"
            else
                log_info "${service_name} 已终止"
            fi
        fi
    else
        log_info "${service_name} (端口 ${port}) 未运行"
    fi
}

stop_services() {
    log_info "=========================================="
    log_info "停止服务"
    log_info "=========================================="
    echo ""
    
    log_info "=== 停止后端服务 ==="
    if ! stop_by_pid_file "${BACKEND_DIR}/.backend.pid" "后端服务"; then
        stop_by_port 8080 "后端服务"
    fi
    echo ""
    
    log_info "=== 停止前端服务 ==="
    if ! stop_by_pid_file "${FRONTEND_DIR}/.frontend.pid" "前端服务"; then
        stop_by_port 5173 "前端服务"
    fi
    echo ""
    
    log_info "=== 清理残留进程 ==="
    local maven_pids
    maven_pids=$(pgrep -f "mvn spring-boot:run" 2>/dev/null || true)
    if [ -n "${maven_pids}" ]; then
        log_warn "发现残留的 Maven 进程: ${maven_pids}"
        for pid in ${maven_pids}; do
            kill -9 "${pid}" 2>/dev/null || true
        done
        log_info "已清理残留进程"
    fi
    
    local vite_pids
    vite_pids=$(pgrep -f "vite" 2>/dev/null || true)
    if [ -n "${vite_pids}" ]; then
        log_warn "发现残留的 Vite 进程: ${vite_pids}"
        for pid in ${vite_pids}; do
            kill -9 "${pid}" 2>/dev/null || true
        done
        log_info "已清理残留进程"
    fi
    echo ""
    
    log_info "停止完成"
    echo ""
}

compile_backend() {
    log_info "开始编译后端项目..."
    cd "${BACKEND_DIR}"
    
    log_info "执行: mvn clean install -DskipTests"
    mvn clean install -DskipTests -q
    
    log_info "后端编译成功"
}

compile_frontend() {
    log_info "开始编译前端项目..."
    cd "${FRONTEND_DIR}"
    
    log_info "执行: npm run build"
    npm run build
    
    log_info "前端编译成功"
}

compile_projects() {
    log_info "=========================================="
    log_info "编译项目"
    log_info "=========================================="
    echo ""
    
    log_info "=== 编译后端项目 ==="
    compile_backend
    echo ""
    
    log_info "=== 编译前端项目 ==="
    compile_frontend
    echo ""
    
    log_info "编译完成"
    echo ""
}

create_log_dirs() {
    mkdir -p "${BACKEND_DIR}/logs"
    mkdir -p "${FRONTEND_DIR}/logs"
}

start_backend() {
    log_info "启动后端服务..."
    cd "${BACKEND_DIR}"
    
    log_info "执行: mvn spring-boot:run (后台运行)"
    nohup mvn spring-boot:run > "${BACKEND_DIR}/logs/server.log" 2>&1 &
    local backend_pid=$!
    
    log_info "后端服务正在启动，PID: ${backend_pid}"
    log_info "日志文件: ${BACKEND_DIR}/logs/server.log"
    
    local max_wait=30
    local wait_count=0
    while ! lsof -n -P -iTCP:8080 -sTCP:LISTEN >/dev/null 2>&1 && [ ${wait_count} -lt ${max_wait} ]; do
        sleep 1
        ((wait_count++))
        if [ $((wait_count % 5)) -eq 0 ]; then
            log_info "等待后端服务启动... (${wait_count}/${max_wait}s)"
        fi
    done
    
    if lsof -n -P -iTCP:8080 -sTCP:LISTEN >/dev/null 2>&1; then
        log_info "后端服务启动成功"
        echo "${backend_pid}" > "${BACKEND_DIR}/.backend.pid"
    else
        log_error "后端服务启动超时 (${max_wait}s)"
        log_error "请检查日志: ${BACKEND_DIR}/logs/server.log"
        exit 1
    fi
}

start_frontend() {
    log_info "启动前端服务..."
    cd "${FRONTEND_DIR}"
    
    log_info "执行: npm run dev (后台运行)"
    nohup npm run dev > "${FRONTEND_DIR}/logs/server.log" 2>&1 &
    local frontend_pid=$!
    
    log_info "前端服务正在启动，PID: ${frontend_pid}"
    log_info "日志文件: ${FRONTEND_DIR}/logs/server.log"
    
    local max_wait=15
    local wait_count=0
    while ! lsof -n -P -iTCP:5173 -sTCP:LISTEN >/dev/null 2>&1 && [ ${wait_count} -lt ${max_wait} ]; do
        sleep 1
        ((wait_count++))
        if [ $((wait_count % 3)) -eq 0 ]; then
            log_info "等待前端服务启动... (${wait_count}/${max_wait}s)"
        fi
    done
    
    if lsof -n -P -iTCP:5173 -sTCP:LISTEN >/dev/null 2>&1; then
        log_info "前端服务启动成功"
        echo "${frontend_pid}" > "${FRONTEND_DIR}/.frontend.pid"
    else
        log_error "前端服务启动超时 (${max_wait}s)"
        log_error "请检查日志: ${FRONTEND_DIR}/logs/server.log"
        exit 1
    fi
}

start_services() {
    log_info "=========================================="
    log_info "启动服务"
    log_info "=========================================="
    echo ""
    
    create_log_dirs
    
    log_info "=== 启动后端服务 ==="
    start_backend
    echo ""
    
    log_info "=== 启动前端服务 ==="
    start_frontend
    echo ""
    
    log_info "=========================================="
    log_info "所有服务启动成功！"
    log_info "=========================================="
    echo ""
    log_info "服务地址："
    log_info "  - 后端 API:  http://localhost:8080/"
    log_info "  - 前端应用:  http://localhost:5173/"
    log_info "  - H2 控制台: http://localhost:8080/h2-console"
    echo ""
    log_info "停止服务命令："
    log_info "  - 停止所有: $0 --stop"
    log_info "  - 手动停止: kill -9 \$(lsof -t -iTCP:8080) 2>/dev/null || true"
    log_info "  - 手动停止: kill -9 \$(lsof -t -iTCP:5173) 2>/dev/null || true"
    echo ""
    log_info "日志文件位置："
    log_info "  - 后端日志: ${BACKEND_DIR}/logs/server.log"
    log_info "  - 前端日志: ${FRONTEND_DIR}/logs/server.log"
    echo ""
}

print_status() {
    echo ""
    log_info "=========================================="
    log_info "服务状态"
    log_info "=========================================="
    echo ""
    
    if lsof -n -P -iTCP:8080 -sTCP:LISTEN >/dev/null 2>&1; then
        local pid
        pid=$(lsof -t -iTCP:8080 -sTCP:LISTEN 2>/dev/null || true)
        log_info "  - 后端服务 (端口 8080): ${GREEN}运行中${NC} (PID: ${pid})"
    else
        log_info "  - 后端服务 (端口 8080): ${RED}未运行${NC}"
    fi
    
    if lsof -n -P -iTCP:5173 -sTCP:LISTEN >/dev/null 2>&1; then
        local pid
        pid=$(lsof -t -iTCP:5173 -sTCP:LISTEN 2>/dev/null || true)
        log_info "  - 前端服务 (端口 5173): ${GREEN}运行中${NC} (PID: ${pid})"
    else
        log_info "  - 前端服务 (端口 5173): ${RED}未运行${NC}"
    fi
    echo ""
}

main() {
    parse_args "$@"
    
    log_info "=========================================="
    log_info "Solo Video Platform 一键管理脚本"
    log_info "=========================================="
    echo ""
    
    if [ "${ACTION_STOP}" = true ]; then
        stop_services
    fi
    
    if [ "${ACTION_COMPILE}" = true ]; then
        compile_projects
    fi
    
    if [ "${ACTION_START}" = true ]; then
        start_services
    fi
    
    print_status
}

main "$@"
