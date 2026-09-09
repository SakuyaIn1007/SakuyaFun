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
export WENKU8_ADAPTER_ENABLED="${WENKU8_ADAPTER_ENABLED:-false}"
export WENKU8_ADAPTER_BASE_URL="$adapter_base_url"

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

echo "启动 Wenku8 适配器：$adapter_base_url（WENKU8_ENABLED=$WENKU8_ENABLED）"
(
  cd "$adapter_dir"
  exec "$python_bin" -m uvicorn app.main:app --host "$adapter_host" --port "$adapter_port"
) >"$adapter_log" 2>&1 &
adapter_pid=$!

health_body="$(mktemp "${TMPDIR:-/tmp}/sakuya-wenku8-health.XXXXXX")"
trap 'rm -f "$health_body"; cleanup' EXIT INT TERM

for _ in {1..30}; do
  if curl --silent --show-error --output /dev/null "$adapter_base_url/health"; then
    break
  fi
  sleep 1
done

if ! curl --silent --show-error --output /dev/null "$adapter_base_url/health"; then
  echo "适配器未在 30 秒内就绪；日志：$adapter_log" >&2
  exit 1
fi

verify_status="$(curl --silent --show-error --output "$health_body" --write-out '%{http_code}' "$adapter_base_url/health?verify=true")"
if [[ "$WENKU8_ENABLED" == "true" ]]; then
  # 开启上游时必须真实完成登录校验；未提供凭据或登录失败都阻止 Spring Boot 启动。
  if [[ "$verify_status" != "200" ]]; then
    echo "Wenku8 上游登录校验失败（HTTP $verify_status）；Spring Boot 未启动。响应：$(<"$health_body")" >&2
    exit 1
  fi
  echo "Wenku8 上游登录校验通过。"
else
  # 默认关闭时 verify=true 应被显式拒绝，证明此一键流程没有悄悄访问上游。
  if [[ "$verify_status" != "503" ]] || ! grep -q 'ADAPTER_DISABLED' "$health_body"; then
    echo "安全关闭校验失败：预期 /health?verify=true 返回 503 ADAPTER_DISABLED，实际为 HTTP $verify_status。" >&2
    exit 1
  fi
  echo "已确认 Wenku8 上游保持关闭（/health?verify=true -> ADAPTER_DISABLED）。"
fi

echo "启动 Spring Boot（WENKU8_ADAPTER_ENABLED=$WENKU8_ADAPTER_ENABLED）"
cd "$repo_root"
# 保持本脚本作为父进程，才能在 Spring Boot 退出或收到 Ctrl+C 时回收适配器进程。
./gradlew -p backend bootRun
