#!/usr/bin/env bash

# run-wenku8-local.sh
# 职责说明：
# 1. 在本地按顺序启动 Wenku8 内部适配器、校验其登录健康状态，再启动 Spring Boot。
# 2. 不读取、不保存账号密码；所有上游访问均须由调用者显式通过环境变量授权。
# 3. 默认验证适配器的“关闭上游”安全分支，防止开发启动意外触发 Wenku8 登录或抓取。
#
# 执行流程：
# 1. 继承调用环境中的 WENKU8_* 变量并启动 Python 适配器；未设置时 WENKU8_ENABLED=false。
# 2. 轮询 /health，随后请求 /health?verify=true：关闭模式必须返回 ADAPTER_DISABLED，开启模式必须返回 200。
# 3. 健康校验通过后，以同一组显式环境变量启动 Spring Boot；脚本退出时终止它启动的适配器进程。

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
adapter_dir="$repo_root/services/wenku8-adapter"
adapter_host="${WENKU8_ADAPTER_HOST:-127.0.0.1}"
adapter_port="${WENKU8_ADAPTER_PORT:-8000}"
adapter_base_url="${WENKU8_ADAPTER_BASE_URL:-http://${adapter_host}:${adapter_port}}"
default_python="$adapter_dir/.venv/bin/python"
# 优先复用适配器隔离环境，未创建虚拟环境时才回退到系统 Python；调用者仍可用 PYTHON_BIN 覆盖。
if [[ ! -x "$default_python" ]]; then default_python="python3"; fi
python_bin="${PYTHON_BIN:-$default_python}"
adapter_log="${WENKU8_ADAPTER_LOG:-${TMPDIR:-/tmp}/sakuya-wenku8-adapter.log}"

# 环境变量只在当前子进程树中生效，绝不写入文件或日志。默认关闭上游访问。
# Python 端对布尔值大小写不敏感，这里同步规范化，避免 TRUE 被误判为关闭模式。
export WENKU8_ENABLED="$(printf '%s' "${WENKU8_ENABLED:-false}" | tr '[:upper:]' '[:lower:]')"
# 两个开关语义完全不同，不可混为一谈：
#   WENKU8_ENABLED        适配器是否访问上游（抓取），默认 false，这是真正的安全边界；
#   WENKU8_ADAPTER_ENABLED 后端是否连接本机适配器服务，只是本机 HTTP，不产生抓取。
# 后者默认必须为 true，否则后端的回退读取会被 AdapterClient 直接短路成 503，
# 表现为「能列目录但点进去读不了」。上游不会被误抓：适配器侧 WENKU8_ENABLED=false 时
# 一律返回 ADAPTER_DISABLED，下面的 verify 断言会强制校验这一点。
export WENKU8_ADAPTER_ENABLED="${WENKU8_ADAPTER_ENABLED:-true}"
export WENKU8_ADAPTER_BASE_URL="$adapter_base_url"

# 封面直连默认跟随 WENKU8_ENABLED，而不是无条件开启。
# 开启理由：本地开发既然已经打开上游，就应顺带把封面预取打开，免得每张封面都经适配器回源、
# 白吃 5 秒/次的节流额度（那份额度是正文导入和详情补抓共用的）。
# 跟随而非写死 true 的理由：封面直连**绕过适配器**，WENKU8_ENABLED=false 这条真正的安全边界
# 拦不住它。关闭态下虽然「目录取不到书 → 没有书可预取」也会让封面阶段空转，
# 但那是**偶然的传递性抑制**，设计文档明确要求不得依赖它。跟随开关则该抑制不再需要。
export WENKU8_COVER_SOURCE_ENABLED="${WENKU8_COVER_SOURCE_ENABLED:-$WENKU8_ENABLED}"

# 内容存储必须钉成绝对路径。配置默认值 ./content-store 是相对路径，会相对进程工作目录解析，
# 而两种启动方式的工作目录并不相同：IDEA 用仓库根，本脚本经 gradlew -p backend 启动后是 backend/。
# 不钉死就会各读一个目录——脚本启动时数据库里记录的正文文件全部读不到，表现为
# 「目录能列出来，但点进去读不了正文」。库中已有对象都以仓库根为基准，这里与之对齐。
export CONTENT_STORAGE_LOCAL_DIR="${CONTENT_STORAGE_LOCAL_DIR:-$repo_root/content-store}"

adapter_pid=""
cleanup() {
  if [[ -n "$adapter_pid" ]] && kill -0 "$adapter_pid" 2>/dev/null; then
    kill "$adapter_pid" 2>/dev/null || true
    wait "$adapter_pid" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

if ! "$python_bin" -c 'import uvicorn' >/dev/null 2>&1; then
  echo "未找到 uvicorn。请先在 services/wenku8-adapter 安装 requirements.txt，或设置 PYTHON_BIN。" >&2
  exit 1
fi

# 端口必须在启动前确认空闲。若已被占用，uvicorn 会绑定失败并立即退出，而端口上的原进程
# 仍会应答 /health 与 /health?verify=true，使后续校验基于别人的进程得出结论：
# 关闭态会被误报为「安全关闭校验失败」，开启态则会静默连上错误的适配器。
# 事后校验进程存活并不可靠（原进程应答很快，竞态下子进程尚未被回收），只能前置判断。
if lsof -nP -iTCP:"${adapter_port}" -sTCP:LISTEN >/dev/null 2>&1; then
  occupied="$(lsof -nP -iTCP:"${adapter_port}" -sTCP:LISTEN 2>/dev/null | tail -n +2 | awk '{print $1" (PID "$2")"}' | paste -sd', ' -)"
  echo "端口 ${adapter_port} 已被占用，无法启动新的适配器：${occupied}" >&2
  echo "请先停止占用者，或用 WENKU8_ADAPTER_PORT 指定其他端口。" >&2
  exit 1
fi

# 变量展开必须显式加花括号：macOS 自带 bash 3.2 不把全角括号等多字节字符当作变量名边界，
# 写成不带花括号的裸展开时，全角括号的首字节会被并入变量名，触发 unbound variable 并中止脚本。
echo "启动 Wenku8 适配器：${adapter_base_url}（WENKU8_ENABLED=${WENKU8_ENABLED}）"
(
  cd "$adapter_dir"
  exec "$python_bin" -m uvicorn app.main:app --host "$adapter_host" --port "$adapter_port"
) >"$adapter_log" 2>&1 &
adapter_pid=$!

health_body="$(mktemp "${TMPDIR:-/tmp}/sakuya-wenku8-health.XXXXXX")"
trap 'rm -f "$health_body"; cleanup' EXIT INT TERM

for _ in {1..30}; do
  # 自己启动的进程若已退出就无需再等：继续轮询只会把「端口被占」误诊成「启动超时」。
  if ! kill -0 "$adapter_pid" 2>/dev/null; then break; fi
  # 轮询期间不输出错误：uvicorn 需要若干秒才能监听，前几次连接失败是预期的瞬态，
  # 带 --show-error 会把正常的启动过程渲染成 curl: (7) 报错。失败原因由下面的存活校验
  # 与超时分支负责打印，那时才具备诊断价值。
  if curl --silent --output /dev/null "$adapter_base_url/health"; then
    break
  fi
  sleep 1
done

# 端口冲突已由上面的前置检查确定性拦下，这里兜住其余启动失败（依赖缺失、导入报错等）：
# 若子进程已退出，端口上的其他进程仍可能应答 /health，使脚本误判为「自己启动成功」。
# 因此必须校验自己启动的进程是否存活，并把真正的失败原因（uvicorn 日志）打出来。
if ! kill -0 "$adapter_pid" 2>/dev/null; then
  echo "适配器进程已退出（PID ${adapter_pid}），启动失败。" >&2
  occupied="$(lsof -nP -iTCP:"${adapter_port}" -sTCP:LISTEN 2>/dev/null | tail -n +2 | awk '{print $1" (PID "$2")"}' | paste -sd', ' -)"
  if [[ -n "$occupied" ]]; then
    echo "同时检测到 ${adapter_port} 被其他进程监听：${occupied}" >&2
  fi
  echo "适配器日志末尾（${adapter_log}）：" >&2
  tail -n 20 "$adapter_log" >&2 || true
  exit 1
fi

if ! curl --silent --show-error --output /dev/null "$adapter_base_url/health"; then
  echo "适配器未在 30 秒内就绪；日志：$adapter_log" >&2
  exit 1
fi

verify_status="$(curl --silent --show-error --output "$health_body" --write-out '%{http_code}' "$adapter_base_url/health?verify=true")"
if [[ "$WENKU8_ENABLED" == "true" ]]; then
  # 开启上游时必须真实完成登录校验；未提供凭据或登录失败都阻止 Spring Boot 启动。
  if [[ "$verify_status" != "200" ]]; then
    echo "Wenku8 上游登录校验失败（HTTP ${verify_status}）；Spring Boot 未启动。响应：$(<"$health_body")" >&2
    exit 1
  fi
  echo "Wenku8 上游登录校验通过。"
else
  # 默认关闭时 verify=true 应被显式拒绝，证明此一键流程没有悄悄访问上游。
  if [[ "$verify_status" != "503" ]] || ! grep -q 'ADAPTER_DISABLED' "$health_body"; then
    echo "安全关闭校验失败：预期 /health?verify=true 返回 503 ADAPTER_DISABLED，实际为 HTTP ${verify_status}。" >&2
    exit 1
  fi
  echo "已确认 Wenku8 上游保持关闭（/health?verify=true -> ADAPTER_DISABLED）。"
fi

echo "启动 Spring Boot（WENKU8_ADAPTER_ENABLED=${WENKU8_ADAPTER_ENABLED}，WENKU8_COVER_SOURCE_ENABLED=${WENKU8_COVER_SOURCE_ENABLED}）"
cd "$repo_root"
# 必须用 --console=plain：bootRun 直到进程结束才返回，Gradle 无从细分进度，rich console 会在
# 「80% EXECUTING > :bootRun」上永久停住并持续刷新该状态行。那个百分比是 Gradle 的任务完成度，
# 与 Spring Boot 无关，却会让已经就绪的应用看起来像卡死在 80%；纯文本模式则完整顺序输出启动日志。
# 保持本脚本作为父进程，才能在 Spring Boot 退出或收到 Ctrl+C 时回收适配器进程。
./gradlew -p backend bootRun --console=plain
