# Sakuya Spring Boot 后端

与仓库中的 Kotlin/Compose 客户端配套，提供 JWT 认证、个人资料、头像上传、好友、会话、实时聊天、首页书目和云端书架接口。

## 启动

项目使用仓库根目录已有的 Gradle Wrapper：

```bash
cd ..
./gradlew -p backend bootRun
```

默认地址为 `http://localhost:8080`，默认启用 `dev` Profile，并连接本机 MySQL 的 `sakuya` 数据库。可使用 `backend/docker-compose.yml` 启动 MySQL；开发环境首次启动会创建以下演示账号：

- `sakuya / password123`（含完整资料、两位好友、两条会话、三本书架书目及一条待处理好友申请）
- `alice / password123`
- `bob / password123`

## Wenku8 本地验证

`dev` 配置中的 Wenku8 网关默认关闭，Python 适配器也默认禁止访问上游。使用仓库根目录的脚本可以按“启动适配器 → 校验 `/health?verify=true` → 启动后端”的顺序完成本地验证：

```bash
./backend/scripts/run-wenku8-local.sh
```

该默认命令会验证安全关闭响应 `503 ADAPTER_DISABLED`，不会访问上游。若已在当前终端提供自己的 Wenku8 凭据，才显式开启两端并执行真实登录验证；账号密码不要提交到仓库：

```bash
WENKU8_ENABLED=true WENKU8_ADAPTER_ENABLED=true \
WENKU8_USERNAME='your-account' WENKU8_PASSWORD='your-password' \
./backend/scripts/run-wenku8-local.sh
```

上游验证不通过时，脚本会停止，Spring Boot 不会启动。详见 [`services/wenku8-adapter/README.md`](../services/wenku8-adapter/README.md)。

Android 模拟器访问宿主机时，将客户端 `BASE_URL` 配成 `http://10.0.2.2:8080/`，WebSocket 配成 `ws://10.0.2.2:8080/ws/chat`。真机需替换为电脑的局域网 IP，并确保手机和电脑在同一网络。

本项目客户端已支持用 Gradle Property 覆盖地址，Debug Manifest 也已允许本机明文连接：

```bash
./gradlew :app:installDebug \
  -PapiBaseUrl=http://10.0.2.2:8080/ \
  -PwsChatUrl=ws://10.0.2.2:8080/ws/chat
```

## MySQL

```bash
SPRING_PROFILES_ACTIVE=mysql \
DB_URL='jdbc:mysql://localhost:3306/sakuya?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai' \
DB_USERNAME=root \
DB_PASSWORD=your-password \
JWT_SECRET='replace-with-a-random-secret-at-least-32-bytes' \
../gradlew bootRun
```

启用 `mysql` 等非开发 Profile 后不会创建演示账号，并且必须通过环境变量提供 `JWT_SECRET`。生产环境还应使用反向代理提供 HTTPS/WSS。

## 动态与好友通知推送

通知历史、未读数和偏好由后端数据库保存；FCM 只是通知栏派发通道。未配置 Firebase 时，通知中心与相关 API 仍可正常开发和测试，Outbox 会由 Noop 通道安全消费。

1. 在 Firebase 控制台创建 Android 应用，包名使用 `com.sakuya.sakuyainandroid`，将 `google-services.json` 放到本机 `app/` 目录。Gradle 仅在检测到该文件时启用 Google Services 插件。
2. 为服务端准备具有 FCM 权限的服务账号，并把 JSON 保存在仓库外。也可使用部署平台提供的 Application Default Credentials。
3. 启动后端时启用 FCM：

```bash
FIREBASE_ENABLED=true \
FIREBASE_CREDENTIALS_PATH='/absolute/secret/firebase-service-account.json' \
JWT_SECRET='replace-with-a-random-secret-at-least-32-bytes' \
../gradlew -p backend bootRun
```

可用 `NOTIFICATION_DISPATCH_DELAY_MS` 调整 Outbox 扫描周期，默认 15000 毫秒。点赞与收藏会按用户和动态聚合两分钟；评论、回复、关注和好友事件立即进入派发队列。任何真实凭证、FCM Token 或 `google-services.json` 都不得提交。

## 内容库与后台导入

Android 只访问 Spring Boot 的 `/content/**` API，不直接连接 MySQL，也不在用户请求期间依赖爬虫。开发环境默认将章节、全文和封面写入 `./content-store`；生产环境可通过下列配置切换到 S3 或 MinIO：

```bash
CONTENT_MANAGEMENT_TOKEN='replace-with-a-strong-token' \
CONTENT_STORAGE_TYPE=s3 \
CONTENT_STORAGE_ENDPOINT='http://minio:9000' \
CONTENT_STORAGE_BUCKET=sakuya-content \
CONTENT_STORAGE_ACCESS_KEY='your-access-key' \
CONTENT_STORAGE_SECRET_KEY='your-secret-key' \
CONTENT_READ_MODE=DB_FIRST_WITH_ADAPTER_FALLBACK \
../gradlew bootRun
```

管理写接口同时需要 JWT 和 `X-Content-Management-Token`。可创建/编辑书目，上传 TXT、EPUB 和封面，维护首页栏目、绑定第三方来源，并启动或查询后台导入任务。正常读取稳定后，将 `CONTENT_READ_MODE` 切换为 `DB_ONLY` 即可完全停止 Python 适配器；旧 `/wenku8/**` 路径在迁移期仍会优先读取内容库。

## 主要接口

除登录、注册、健康检查和静态头像外，其余 HTTP 接口均需要请求头 `Authorization: Bearer <token>`。

| 功能 | 接口 |
| --- | --- |
| 登录 / 注册 | `POST /auth/login`, `POST /auth/register` |
| 个人资料 | `GET/PUT /profile`, `POST /profile/upload/avatar` |
| 好友 | `GET /friends`, `GET /friends/search`, `POST/GET /friends/requests` |
| 处理好友申请 | `PUT /friends/requests/{id}/accept|reject` |
| 通知历史 / 未读 | `GET /notifications`, `GET /notifications/unread-count|unread-summary`, `PUT /notifications/{id}/read`, `PUT /notifications/read-all|read-category` |
| 推送设备 / 偏好 | `POST/DELETE /notifications/devices`, `GET/PUT /notifications/preferences` |
| 关注动态提示 | `GET /feed/following/unseen-summary`, `PUT /feed/following/read`, `PUT /feed/following/authors/{authorId}/read` |
| 作者主页 / 作者动态 | `GET /profiles/{userId}`, `GET /feed/authors/{authorId}?page=&pageSize=` |
| 会话 / 消息 | `GET /conversations`, `GET/POST /conversations/{id}/messages` |
| 聊天附件 | `POST /conversations/{id}/attachments`, `GET/DELETE /conversations/{id}/attachments/{attachmentId}[/content]` |
| 轻小说时间表 | `GET /novels/releases`，返回已公布更新日期、卷信息与推荐标记 |
| 好友单聊 | `POST /friends/{friendId}/conversation` |
| 消息已读 | `PUT /conversations/{id}/read` |
| 实时聊天 | `WS /ws/chat`，握手时携带 Bearer Token |
| 首页 | `GET /home/recommendations|novels|rankings` |
| 图书 | `GET /books/{id}`, `GET /books/search` |
| 统一搜索 / 热词 | `GET /search?keyword=&type=&page=&pageSize=`, `GET /search/hot-keywords` |
| 示例阅读内容 | `GET /books/{id}/content.txt` |
| 云端书架 | `GET /library`, `POST/DELETE /library/{bookId}` |
| 内容库搜索 / 详情 | `GET /content/novels`, `GET /content/novels/{bookId}` |
| 内容目录 / 单章 / 全文 | `GET /content/novels/{bookId}/chapters`, `GET /content/chapters/{chapterId}`, `GET /content/novels/{bookId}/full-content` |
| 内容管理 | `POST/PUT /admin/content/books`, `POST /admin/content/books/{bookId}/document|cover` |
| 后台导入 | `POST /admin/content/imports`, `GET /admin/content/imports/{jobId}` |

所有 HTTP 响应与 Android 端 `BaseResponse<T>` 对齐：

```json
{"code": 200, "message": "success", "data": {}}
```

## 测试

```bash
./gradlew -p backend test
```
