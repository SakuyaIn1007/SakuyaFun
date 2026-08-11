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

## 主要接口

除登录、注册、健康检查和静态头像外，其余 HTTP 接口均需要请求头 `Authorization: Bearer <token>`。

| 功能 | 接口 |
| --- | --- |
| 登录 / 注册 | `POST /auth/login`, `POST /auth/register` |
| 个人资料 | `GET/PUT /profile`, `POST /profile/upload/avatar` |
| 好友 | `GET /friends`, `GET /friends/search`, `POST/GET /friends/requests` |
| 处理好友申请 | `PUT /friends/requests/{id}/accept|reject` |
| 会话 / 消息 | `GET /conversations`, `GET/POST /conversations/{id}/messages` |
| 轻小说时间表 | `GET /novels/releases`，返回已公布更新日期、卷信息与推荐标记 |
| 好友单聊 | `POST /friends/{friendId}/conversation` |
| 消息已读 | `PUT /conversations/{id}/read` |
| 实时聊天 | `WS /ws/chat`，握手时携带 Bearer Token |
| 首页 | `GET /home/recommendations|novels|rankings` |
| 图书 | `GET /books/{id}`, `GET /books/search` |
| 示例阅读内容 | `GET /books/{id}/content.txt` |
| 云端书架 | `GET /library`, `POST/DELETE /library/{bookId}` |

所有 HTTP 响应与 Android 端 `BaseResponse<T>` 对齐：

```json
{"code": 200, "message": "success", "data": {}}
```

## 测试

```bash
./gradlew -p backend test
```
