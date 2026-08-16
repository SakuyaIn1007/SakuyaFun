# Wenku8 内部适配服务

本服务仅供 SakuyaInAndroid 的内部验证环境使用：Spring Boot 通过内网调用本服务，Android 客户端不得直接访问。

它封装 [pywenku8api](https://github.com/WorldObservationLog/pywenku8api)，将上游网页数据转换为稳定的 JSON 协议。该依赖为 AGPL-3.0；在任何公开部署或分发前，必须完成许可证、源代码提供义务及内容授权评估。

运行：`uvicorn app.main:app --host 0.0.0.0 --port 8000`。通过 `WENKU8_ENABLED=true` 显式启用对上游的调用；默认关闭，避免意外抓取。

连续阅读内部接口为 `GET /novels/{id}/full-content`。它返回小说标题、整本纯文本和目录章节的字符偏移锚点（`offset=-1` 表示上游 TXT 与目录无法对应，调用方应安全降级）。全文只保留在 pywenku8api 与适配服务的内存缓存中，默认 30 分钟，不会写入数据库或本地文件。大正文读取默认最长等待 180 秒，可按部署环境用 `WENKU8_FULL_CONTENT_TIMEOUT_SECONDS` 调整；缓存时长可用 `WENKU8_FULL_CONTENT_CACHE_SECONDS` 调整。

登录流程需要由 `zendriver` 拉起 Chrome。macOS 默认使用 `/Applications/Google Chrome.app/Contents/MacOS/Google Chrome`；其他环境可设置 `WENKU8_BROWSER_PATH` 指向 Chrome/Chromium 可执行文件。排障时可设置 `WENKU8_BROWSER_HEADLESS=false` 观察浏览器是否成功启动。
