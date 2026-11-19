#!/usr/bin/env bash
#
# point single-machine launcher.
#
#   ./point.sh build [--frontend] [--fast]
#   ./point.sh start [--profile dev|prod] [--port 8082] [--cloud]
#   ./point.sh run   [...]          # foreground
#   ./point.sh stop | restart | status | logs
#
# Configuration lives in point-boot/src/main/resources/application*.json.
# The script only overrides what you pass here; see DEPLOYMENT.md.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

PID_FILE="${PID_FILE:-logs/point.pid}"
LOG_FILE="${LOG_FILE:-logs/point.out}"
# Generated per start (0600); holds secrets so they stay out of argv.
RUNTIME_CONFIG="${RUNTIME_CONFIG:-logs/point.runtime.json}"
# Operator-supplied config file (0600) — used instead of generating one.
POINT_CONFIG="${POINT_CONFIG:-}"
PROFILE_EXPLICIT=0
if [[ -n "${PROFILE:-}" ]]; then PROFILE_EXPLICIT=1; fi
PROFILE="${PROFILE:-dev}"
PORT_EXPLICIT=0
if [[ -n "${PORT:-}" ]]; then PORT_EXPLICIT=1; fi
PORT="${PORT:-8082}"
MODE_EXPLICIT=0
if [[ -n "${MODE:-}" ]]; then MODE_EXPLICIT=1; fi
MODE="${MODE:-monolith}"          # monolith | cloud
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx512m}"
JAR="${JAR:-}"
FOREGROUND=0
WITH_FRONTEND=0
FAST_BUILD=0
ASSUME_YES=0

usage() {
  cat <<'USAGE'
用法:
  ./point.sh build [--frontend] [--fast]
  ./point.sh start [--profile dev|prod] [--port 8082] [--cloud|--monolith] [--jar JAR]
  ./point.sh cloud [start 选项]   以 cloud 模式启动（等价 start --cloud）
  ./point.sh run   [同上]         前台运行，Ctrl+C 停止
  ./point.sh restart [--yes] [start 选项]  停止后重新启动；未运行时直接启动（默认单机）
  ./point.sh stop | status | logs | help

常用环境变量:
  PROFILE=dev|prod        配置 profile，默认 dev
  PORT=8082               HTTP 端口
  JAVA_OPTS="-Xms256m -Xmx512m"
  JWT_SECRET=...          覆盖 bbs.jwt.secret（prod 必须）
  DB_URL=...              覆盖 F2.db.url
  ADMIN_USERNAME/PASSWORD 首次启动时创建管理员（密码至少 8 位）
  STATIC_SERVICES=...     cloud 模式的静态发现覆盖
  MODE=monolith|cloud     启动模式，默认 monolith
  JAR=path/to/point-boot.jar
  POINT_CONFIG=...        使用你自己的 0600 配置文件，而不是生成 logs/point.runtime.json

  上面的密钥类变量会写入 0600 的运行时配置文件（logs/point.runtime.json），
  不再出现在 java 命令行里（-D 参数对所有本机用户可见）。
  自定义 key 也可以直接写进 POINT_CONFIG 指向的 JSON（点号分隔或嵌套均可）。

  --yes / -y              restart 切换模式时跳过确认（非交互环境必须）

示例:
  ./point.sh build --frontend && ./point.sh start
  ./point.sh restart --profile prod
  ./point.sh restart --cloud --yes
  DB_URL='jdbc:h2:file:./data/bbs;MODE=PostgreSQL;DB_CLOSE_DELAY=-1' \
    JWT_SECRET="$(openssl rand -base64 48)" \
    ./point.sh start --profile prod
  ./point.sh cloud --profile dev
  ./point.sh start --cloud --profile dev   # 等价写法；cloud 必须显式指定
USAGE
}

log()  { printf '\033[1;36m[point]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[point]\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[1;31m[point]\033[0m %s\n' "$*" >&2; exit 1; }

find_jar() {
  if [[ -n "$JAR" ]]; then
    [[ -f "$JAR" ]] || die "JAR 不存在: $JAR"
    printf '%s\n' "$JAR"
    return
  fi
  local candidate
  candidate="$(ls -t point-boot/target/point-boot-*.jar 2>/dev/null | head -n 1 || true)"
  [[ -n "$candidate" ]] || die "未找到 JAR，请先执行 ./point.sh build"
  printf '%s\n' "$candidate"
}

require_java() {
  command -v java >/dev/null 2>&1 || die "未找到 java，请先安装 JDK 25"
  local raw major
  raw="$(java -version 2>&1 | awk -F'"' '/version/ {print $2; exit}')"
  major="${raw%%.*}"
  [[ "$major" == "1" ]] && major="$(printf '%s' "$raw" | cut -d. -f2)"
  if [[ -n "$major" && "$major" -lt 25 ]] 2>/dev/null; then
    die "需要 JDK 25+，当前为 $raw"
  fi
}

detected_mode() {
  local pid
  pid="$(cat "$PID_FILE" 2>/dev/null || true)"
  [[ -n "$pid" ]] || return 0
  if ps -p "$pid" -o command= 2>/dev/null | grep -q 'PointCloudApp'; then
    printf 'cloud\n'
  else
    printf 'monolith\n'
  fi
}

detected_port() {
  local pid
  pid="$(cat "$PID_FILE" 2>/dev/null || true)"
  [[ -n "$pid" ]] || return 0
  ps -p "$pid" -o command= 2>/dev/null \
    | sed -n 's/.*-DF2\.http\.server\.port=\([0-9][0-9]*\).*/\1/p' \
    | head -n 1
}

is_running() {
  [[ -f "$PID_FILE" ]] || return 1
  local pid
  pid="$(cat "$PID_FILE" 2>/dev/null || true)"
  [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null
}

health_url() {
  # Cloud instances are only useful once their dependencies answer, so the
  # startup wait (and `status`) uses the readiness probe — it aggregates the
  # application's CloudHealthContributor (database) and answers 503 while a
  # dependency is unreachable. /health/live only proves the process is up.
  if [[ "$MODE" == "cloud" ]]; then
    printf 'http://127.0.0.1:%s/health/ready\n' "$PORT"
  else
    printf 'http://127.0.0.1:%s/healthz\n' "$PORT"
  fi
}

http_ok() {
  local url="$1"
  if command -v curl >/dev/null 2>&1; then
    curl -fsS --max-time 2 "$url" >/dev/null 2>&1
  elif command -v wget >/dev/null 2>&1; then
    wget -q -T 2 -O /dev/null "$url"
  else
    return 2
  fi
}

confirm() {
  local prompt="$1"
  if [[ "$ASSUME_YES" == "1" ]]; then
    return 0
  fi
  if [[ ! -t 0 ]]; then
    warn "非交互环境，无法确认；确定切换请加 --yes"
    return 1
  fi
  local reply
  printf '%s [y/N] ' "$prompt" >&2
  read -r reply || return 1
  case "$reply" in
    y|Y|yes|YES|Yes) return 0 ;;
    *) return 1 ;;
  esac
}

build_app() {
  if [[ "$WITH_FRONTEND" == "1" ]]; then
    [[ -d point-frontend/node_modules ]] || die "前端依赖未安装，请先执行: cd point-frontend && npm install"
    log "构建前端并同步到 point-boot 静态资源..."
    bash tools/sync-frontend.sh
  fi
  local args=(clean package -pl point-boot -am)
  [[ "$FAST_BUILD" == "1" ]] && args+=(-DskipTests)
  log "构建后端: mvn ${args[*]}"
  if ! mvn -o "${args[@]}"; then
    warn "离线构建失败，重试联网构建..."
    mvn "${args[@]}"
  fi
}

json_escape() {
  local s="$1"
  s="${s//\\/\\\\}"
  s="${s//\"/\\\"}"
  s="${s//$'\n'/\\n}"
  s="${s//$'\r'/\\r}"
  s="${s//$'\t'/\\t}"
  printf '%s' "$s"
}

# Writes the environment overrides that must not sit on the JVM command line
# (`ps` shows argv to every local user) into a 0600 JSON file. freeway loads the
# file named by FREEWAY_CONFIG_FILE at the top of its file tier, so the values
# reach the app exactly as the old -D flags did.
# Returns 1 when there is nothing to write (no file is created).
write_runtime_config() {
  local path="$1"
  local -a entries=()
  [[ -n "${DB_URL:-}" ]] && entries+=("\"freeway.db.url\": \"$(json_escape "$DB_URL")\"")
  [[ -n "${DB_USERNAME:-}" ]] && entries+=("\"freeway.db.username\": \"$(json_escape "$DB_USERNAME")\"")
  [[ -n "${DB_PASSWORD:-}" ]] && entries+=("\"freeway.db.password\": \"$(json_escape "$DB_PASSWORD")\"")
  [[ -n "${JWT_SECRET:-}" ]] && entries+=("\"bbs.jwt.secret\": \"$(json_escape "$JWT_SECRET")\"")
  [[ -n "${ADMIN_USERNAME:-}" ]] && entries+=("\"bbs.admin.username\": \"$(json_escape "$ADMIN_USERNAME")\"")
  [[ -n "${ADMIN_PASSWORD:-}" ]] && entries+=("\"bbs.admin.password\": \"$(json_escape "$ADMIN_PASSWORD")\"")
  [[ -n "${UPLOAD_DIR:-}" ]] && entries+=("\"bbs.upload.dir\": \"$(json_escape "$UPLOAD_DIR")\"")
  [[ -n "${STATIC_SERVICES:-}" ]] && entries+=("\"freeway.cloud.discovery.static.services\": \"$(json_escape "$STATIC_SERVICES")\"")
  [[ ${#entries[@]} -gt 0 ]] || return 1

  mkdir -p "$(dirname "$path")"
  (
    umask 077
    {
      printf '{\n'
      local i
      for i in "${!entries[@]}"; do
        printf '  %s' "${entries[$i]}"
        [[ "$i" -lt $((${#entries[@]} - 1)) ]] && printf ','
        printf '\n'
      done
      printf '}\n'
    } > "$path"
  )
  chmod 600 "$path"
  log "运行时配置写入 $path（0600）"
  return 0
}

start_app() {
  if is_running; then
    log "point 已在运行 (PID $(cat "$PID_FILE"))"
    status_app
    return 0
  fi
  local jar; jar="$(find_jar)"
  require_java
  local -a java_opts=()
  if [[ -n "$JAVA_OPTS" ]]; then
    read -r -a java_opts <<< "$JAVA_OPTS"
  fi

  local -a jvm_args=()
  if [[ ${#java_opts[@]} -gt 0 ]]; then
    jvm_args+=("${java_opts[@]}")
  fi
  # Non-secret values stay on the command line: point.sh's detected_port()
  # reads the port back out of `ps`.
  jvm_args+=("-DF2.http.server.port=$PORT")
  [[ -n "${HOST:-}" ]] && jvm_args+=("-DF2.http.server.host=$HOST")

  # Everything else (credentials included) goes through a config file.
  local config_file="${POINT_CONFIG:-}"
  if [[ -n "$config_file" ]]; then
    [[ -f "$config_file" ]] || die "POINT_CONFIG 指向的文件不存在: $config_file"
    local perms
    perms="$(stat -f '%Lp' "$config_file" 2>/dev/null || stat -c '%a' "$config_file" 2>/dev/null || printf '')"
    [[ -z "$perms" || "$perms" == "600" ]] || warn "POINT_CONFIG 建议设为 0600: chmod 600 $config_file"
  else
    config_file="$RUNTIME_CONFIG"
    write_runtime_config "$config_file" || config_file=""
  fi
  if [[ -n "$config_file" ]]; then
    export FREEWAY_CONFIG_FILE="$config_file"
  fi

  local main="com.jujin.point.boot.PointApp"
  local -a app_args=()
  if [[ "$MODE" == "cloud" ]]; then
    main="com.jujin.point.boot.PointCloudApp"
    [[ -z "${STATIC_SERVICES:-}" ]] && warn "cloud 模式使用 profile 中的 F2.cloud.discovery.static.services；若端口/地址不同，请用 STATIC_SERVICES 覆盖"
  fi
  if [[ -n "$PROFILE" && "$PROFILE" != "none" ]]; then
    app_args+=("--F2.profile=$PROFILE")
  fi

  log "profile=${PROFILE:-none} mode=$MODE port=$PORT"
  log "jar=$jar"
  if [[ "$FOREGROUND" == "1" ]]; then
    exec java "${jvm_args[@]}" -cp "$jar" "$main" ${app_args[@]+"${app_args[@]}"}
  fi

  : > "$LOG_FILE"
  nohup java "${jvm_args[@]}" -cp "$jar" "$main" ${app_args[@]+"${app_args[@]}"} >> "$LOG_FILE" 2>&1 &
  local pid=$!
  printf '%s\n' "$pid" > "$PID_FILE"
  log "已启动 PID ${pid}，日志 $LOG_FILE"

  if ! command -v curl >/dev/null 2>&1 && ! command -v wget >/dev/null 2>&1; then
    log "未找到 curl/wget，跳过健康检查；请查看 $LOG_FILE"
    return 0
  fi

  local url; url="$(health_url)"
  local i
  for i in $(seq 1 30); do
    if http_ok "$url"; then
      log "启动完成: http://localhost:$PORT"
      return 0
    fi
    if ! kill -0 "$pid" 2>/dev/null; then
      warn "启动失败，最后 40 行日志:"
      tail -n 40 "$LOG_FILE" >&2 || true
      rm -f "$PID_FILE"
      exit 1
    fi
    sleep 1
  done
  warn "进程已启动，但健康检查未在 30 秒内通过: $url"
  warn "请查看 $LOG_FILE 或 logs/point.0.log"
}

stop_app() {
  if ! is_running; then
    log "point 未运行"
    rm -f "$PID_FILE"
    [[ -z "${POINT_CONFIG:-}" ]] && rm -f "$RUNTIME_CONFIG"
    return 0
  fi
  local pid; pid="$(cat "$PID_FILE")"
  log "停止 PID $pid ..."
  kill "$pid" 2>/dev/null || true
  local i
  for i in $(seq 1 20); do
    kill -0 "$pid" 2>/dev/null || break
    sleep 1
  done
  if kill -0 "$pid" 2>/dev/null; then
    warn "未在 20 秒内退出，强制结束"
    kill -9 "$pid" 2>/dev/null || true
  fi
  rm -f "$PID_FILE"
  # The generated file holds secrets; it is regenerated on the next start.
  [[ -z "${POINT_CONFIG:-}" ]] && rm -f "$RUNTIME_CONFIG"
  log "已停止"
}

restart_app() {
  local running_mode=""
  if is_running; then
    running_mode="$(detected_mode)"
  fi

  if [[ "$MODE_EXPLICIT" == "1" && "$MODE" == "cloud" ]] \
      && is_running && [[ "$running_mode" != "cloud" ]]; then
    if ! confirm "当前运行的是单机模式，确认切换到 cloud 模式重启？"; then
      log "已取消，point 保持当前进程不变"
      return 1
    fi
  fi

  if [[ "$MODE_EXPLICIT" == "0" && "$running_mode" == "cloud" ]]; then
    warn "当前运行的是 cloud 模式；未指定 --cloud，将按默认单机模式重启"
  fi

  log "重启 point (mode=$MODE port=$PORT profile=$PROFILE)..."
  stop_app
  start_app
}

status_app() {
  if is_running; then
    if [[ "$MODE_EXPLICIT" == "0" ]]; then
      local running_mode
      running_mode="$(detected_mode)"
      [[ -n "$running_mode" ]] && MODE="$running_mode"
    fi
    if [[ "$PORT_EXPLICIT" == "0" ]]; then
      local running_port
      running_port="$(detected_port)"
      [[ -n "$running_port" ]] && PORT="$running_port"
    fi
  fi
  if ! is_running; then
    [[ -f "$PID_FILE" ]] && rm -f "$PID_FILE"
    log "point 未运行"
    return 1
  fi
  local pid; pid="$(cat "$PID_FILE")"
  log "point 运行中 PID ${pid}，端口 $PORT"
  local url; url="$(health_url)"
  if command -v curl >/dev/null 2>&1 || command -v wget >/dev/null 2>&1; then
    if http_ok "$url"; then
      log "健康检查: OK ($url)"
    else
      warn "健康检查失败: $url"
    fi
  fi
  return 0
}

logs_app() {
  if [[ -f "$LOG_FILE" ]]; then
    exec tail -n 100 -f "$LOG_FILE"
  fi
  local latest
  latest="$(ls -t logs/point.*.log 2>/dev/null | head -n 1 || true)"
  [[ -n "$latest" ]] || die "未找到日志文件"
  exec tail -n 100 -f "$latest"
}

CMD="${1:-start}"
if [[ "$CMD" == -* ]]; then
  CMD="start"
else
  shift || true
fi
if [[ "$CMD" == "cloud" ]]; then
  MODE="cloud"
  MODE_EXPLICIT=1
  CMD="start"
fi

while [[ $# -gt 0 ]]; do
  case "$1" in
    --profile)   PROFILE="${2:?}"; PROFILE_EXPLICIT=1; shift 2 ;;
    --profile=*) PROFILE="${1#*=}"; PROFILE_EXPLICIT=1; shift ;;
    --port)      PORT="${2:?}"; PORT_EXPLICIT=1; shift 2 ;;
    --port=*)    PORT="${1#*=}"; PORT_EXPLICIT=1; shift ;;
    --jar)       JAR="${2:?}"; shift 2 ;;
    --jar=*)     JAR="${1#*=}"; shift ;;
    --cloud)     MODE="cloud"; MODE_EXPLICIT=1; shift ;;
    --monolith)  MODE="monolith"; MODE_EXPLICIT=1; shift ;;
    cloud)       MODE="cloud"; MODE_EXPLICIT=1; shift ;;
    monolith)    MODE="monolith"; MODE_EXPLICIT=1; shift ;;
    --yes|-y)    ASSUME_YES=1; shift ;;
    --foreground|-f) FOREGROUND=1; shift ;;
    --frontend)  WITH_FRONTEND=1; shift ;;
    --fast)      FAST_BUILD=1; shift ;;
    -h|--help)   usage; exit 0 ;;
    *)           die "未知参数: $1（用 --help 查看用法）" ;;
  esac
done

case "$CMD" in
  build)   build_app ;;
  start)   start_app ;;
  run)     FOREGROUND=1; start_app ;;
  stop)    stop_app ;;
  restart) restart_app ;;
  status)  status_app ;;
  logs)    logs_app ;;
  help|-h|--help) usage ;;
  *)       usage; die "未知命令: $CMD" ;;
esac
