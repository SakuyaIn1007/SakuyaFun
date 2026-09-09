# Wenku8 内部适配服务

本服务仅供 SakuyaInAndroid 的内部验证环境使用：Spring Boot 通过内网调用本服务，Android 客户端不得直接访问。

它封装 [pywenku8api](https://github.com/WorldObservationLog/pywenku8api)，将上游网页数据转换为稳定的 JSON 协议。该依赖为 AGPL-3.0；在任何公开部署或分发前，必须完成许可证、源代码提供义务及内容授权评估。

运行：`uvicorn app.main:app --host 0.0.0.0 --port 8000`。通过 `WENKU8_ENABLED=true` 显式启用对上游的调用；默认关闭，避免意外抓取。

## 本地一键验证

仓库提供 [`backend/scripts/run-wenku8-local.sh`](../../backend/scripts/run-wenku8-local.sh)。脚本不包含、也不会落盘 Wenku8 账号密码；它依次启动适配器，调用 `/health?verify=true`，通过后才启动 Spring Boot。

默认直接运行时，脚本会确认 `verify=true` 返回 `503 ADAPTER_DISABLED`，这表示上游仍被安全关闭，随后以禁用状态启动后端：

```bash
./backend/scripts/run-wenku8-local.sh
```

若要进行真实上游验证，请仅在当前终端显式提供凭据及双端开关；凭据不会输出到脚本日志或写入配置文件：

```bash
WENKU8_ENABLED=true \
WENKU8_ADAPTER_ENABLED=true \
WENKU8_USERNAME='your-account' \
WENKU8_PASSWORD='your-password' \
./backend/scripts/run-wenku8-local.sh
```

真实验证失败时，脚本不会启动 Spring Boot。脚本优先使用 `services/wenku8-adapter/.venv/bin/python`，首次使用前请在该环境中安装 `requirements.txt`；可用 `PYTHON_BIN=/path/to/python` 指定其他解释器。适配器日志默认写入系统临时目录，可用 `WENKU8_ADAPTER_LOG` 覆盖。

连续阅读内部接口为 `GET /novels/{id}/full-content`。它返回小说标题、整本纯文本和目录章节的字符偏移锚点（`offset=-1` 表示上游 TXT 与目录无法对应，调用方应安全降级）。全文只保留在 pywenku8api 与适配服务的内存缓存中，默认 30 分钟，不会写入数据库或本地文件。大正文读取默认最长等待 180 秒，可按部署环境用 `WENKU8_FULL_CONTENT_TIMEOUT_SECONDS` 调整；缓存时长可用 `WENKU8_FULL_CONTENT_CACHE_SECONDS` 调整。

登录流程需要由 `zendriver` 拉起 Chrome。macOS 默认使用 `/Applications/Google Chrome.app/Contents/MacOS/Google Chrome`；其他环境可设置 `WENKU8_BROWSER_PATH` 指向 Chrome/Chromium 可执行文件。排障时可设置 `WENKU8_BROWSER_HEADLESS=false` 观察浏览器是否成功启动。
