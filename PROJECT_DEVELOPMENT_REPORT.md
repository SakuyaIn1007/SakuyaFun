# SakuyaInAndroid 项目功能缺陷审查与开发文档

生成日期：2026-06-18

## 1. 审查结论

项目当前可以编译通过，执行命令：

```bash
./gradlew :app:compileDebugKotlin
```

结果为 `BUILD SUCCESSFUL`。编译过程中 Kotlin daemon 因沙箱权限无法写入 `~/Library/Application Support/kotlin`，Gradle 自动回退到非 daemon 编译，这不是业务代码编译错误，但会让本机编译日志非常嘈杂、速度变慢。

当前项目已经具备多模块结构、Hilt、Retrofit、Room、DataStore、Navigation Compose、Compose UI 组件等基础设施。主要问题不是“不能编译”，而是多个功能处于“UI 或接口轮廓已存在，但真实流程没有闭环”的阶段。

## 2. 模块现状

| 模块 | 当前状态 | 主要风险 |
| --- | --- | --- |
| `app` | 主导航、登录态入口、底部导航已实现 | Debug 默认跳过登录；底部导航选中态传参错误 |
| `core:data` | Retrofit、OkHttp、TokenStorage、Room DB 已有 | Base URL 和 WebSocket URL 硬编码；拦截器同步阻塞读 DataStore |
| `core:di` | 数据库与 TokenStorage 绑定已实现 | Room 没有迁移策略，升级 schema 会崩 |
| `core:navigation` | 已有跨模块路由常量 | 路由参数未编码，中文/特殊字符标题可能破坏路由 |
| `feature:authentication` | 登录/注册流程较完整 | 错误处理简化；注册成功即跳转但可能没有 token |
| `feature:home` | ViewModel/Repository/DataSource 已有 | 绑定 mock 数据源；搜索 UI 没接入 ViewModel；错误态不展示 |
| `feature:library` | 收藏页 UI 和本地 StateFlow 已有 | 无 Repository/API/本地持久化，删除只影响内存 |
| `feature:conversation` | 会话、聊天、WebSocket 轮廓已出现 | 会话列表仍是 mock；WebSocket 状态错误；消息发送失败无反馈；不落库 |
| `feature:friend` | 好友 API 接口、VM、列表 UI 已有 | Repository 完全 mock；添加好友/通知/群聊页面为空 |
| `feature:profile` | 个人页、编辑页、头像上传有雏形 | 资料缓存不可靠；多个编辑页没有保存；直接修改可变字段不会稳定刷新 UI |
| `feature:profile-services` | 设置等页面有 UI | 大量点击项仍是 TODO；退出登录未接 TokenStorage |
| `feature:reader` | TXT/EPUB 打开和阅读 UI 已有 | 进度保存/恢复为空；错误态为空白；文件扩展名判断不稳 |

## 3. P0 缺陷：优先修复

### 3.1 Debug 构建默认跳过登录

位置：`app/build.gradle`

`debug` 中 `DEV_SKIP_AUTH` 为 `true`，`MainActivityViewModel` 会直接进入 `PROFILE_ROUTE`。这会导致调试时认证流程、Token 失效、未登录拦截等问题被长期绕过。

建议：

- 保留开发开关，但默认设为 `false`。
- 用 Gradle property 控制，例如 `-PdevSkipAuth=true`。
- 加一个明显的 Debug UI 标识，避免误判真实登录态。

### 3.2 底部导航组件永远未选中

位置：`app/src/main/java/com/sakuya/sakuyainandroid/MainScreen.kt`

`selected` 已计算，但 `NavigationBarItem(selected = false)` 固定为 false。视觉颜色被手动改了，但 Material 组件状态、无障碍语义和 ripple/indicator 都不正确。

建议：

```kotlin
NavigationBarItem(
    selected = selected,
    onClick = { onNavigate(item.route) },
    ...
)
```

同时 `popUpTo(PROFILE_ROUTE)` 对所有 tab 固定回退到 Profile，建议改为导航图 start destination 或根据顶级 tab 统一策略处理。

### 3.3 个人资料编辑没有形成可靠保存链路

位置：

- `feature/profile/src/main/java/com/sakuya/profile/navigation/ProfileNavigation.kt`
- `feature/profile/src/main/java/com/sakuya/profile/viewmodel/ProfileMeViewModel.kt`
- `feature/profile/src/main/java/com/sakuya/profile/data/repository/UserRepository.kt`

问题：

- 修改名称、电话、性别、地区、签名等页面多数 `onSave` 只是 `popBackStack()`，没有调用保存方法。
- 电话和性别直接修改 `profile.phoneNumber`、`profile.gender` 这类 `var` 字段，不一定触发 StateFlow 更新，也不会落库或请求后端。
- `UserRepository` 使用实例内 `cachedProfile`，且未标 `@Singleton`，多个 ViewModel 注入时可能拿到不同缓存。
- 头像上传成功只更新 `UploadState.Success`，没有把 `avatarUrl` 写回 `userProfile`。

建议：

- `UserProfile` 改为不可变字段，移除 `var`。
- 在 `ProfileMeViewModel` 提供 `updateName/updatePhone/updateGender/updateRegion/updateSignature/updateAvatar`。
- `UserRepository` 接入真实 API 或 Room，并统一作为单一数据源。
- 保存成功后更新 StateFlow，失败时展示错误。

### 3.4 会话/好友数据层仍然实际使用 mock

位置：

- `feature/conversation/src/main/java/com/sakuya/conversation/data/repository/ConversationRepository.kt`
- `feature/friend/src/main/java/com/sakuya/friend/data/repository/FriendRepository.kt`
- `feature/home/src/main/java/com/sakuya/home/di/HomeModule.kt`

虽然 API Service 已定义，但 Repository 返回固定列表或内存列表，真实后端数据没有进入 UI。

建议：

- Repository 先读本地 Room 缓存，再请求远端刷新。
- 所有 mock 数据源只在 `debug/mock` flavor 使用。
- 给 Repository 增加明确的 `Result`/错误态，UI 展示 retry。

### 3.5 好友/通知/群聊目标页为空

位置：`feature/friend/src/main/java/com/sakuya/friend/navigation/FriendNavigation.kt`

`FRIEND_ADD_ROUTE`、`FNOTICE_ROUTE`、`GROUP_ROUTE` 都是空 composable。用户点击添加好友、通知、群聊后会进入空白页。

建议：

- 至少补齐占位错误页和返回按钮。
- 第一阶段实现添加好友页：搜索输入、结果列表、发送申请、发送状态。
- 第二阶段实现好友请求页：待处理列表、接受、拒绝。

## 4. P1 缺陷：高优先级

### 4.1 WebSocket 状态与重连不完整

位置：`feature/conversation/src/main/java/com/sakuya/conversation/data/remote/ChatWebSocket.kt`

问题：

- `onOpen` 设置为 `CONNECTING`，应该是 `CONNECTED`。
- `scheduleReconnect()` 为空。
- 没有 `onClosing/onClosed` 状态处理。
- token 为空时仍发送 `Bearer null`。
- URL `wss://api.sakuya.com/ws/chat` 硬编码。

建议：

- 补齐连接状态机：Disconnected、Connecting、Connected、Reconnecting、Failed。
- 指数退避重连，并在 ViewModel 暴露状态。
- WebSocket URL 从 BuildConfig 注入。

### 4.2 聊天消息发送没有失败反馈和本地回显策略

位置：`feature/conversation/src/main/java/com/sakuya/conversation/viewmodel/ChatViewModel.kt`

当前发送逻辑清空输入框后直接调用 WebSocket。若连接失败、服务端拒绝、消息未回执，用户无法知道失败。

建议：

- 本地先插入 pending 消息。
- 服务端 ack 后更新为 sent。
- 失败时标记 failed，支持重发。
- 收到消息后写入 Room，并更新会话摘要和未读数。

### 4.3 搜索栏没有接入 HomeViewModel

位置：

- `feature/home/src/main/java/com/sakuya/home/ui/HomeScreen.kt`
- `feature/home/src/main/java/com/sakuya/home/ui/components/HomeTopBar.kt`

`HomeContent` 接收 `onSearchQueryChanged/onSearchClear`，但 `HomeTopBar` 自己 `remember` 了 `searchQuery`，没有向外传递输入变化。

建议：

- `HomeTopBar(query, onQueryChanged, onClear)` 改为受控组件。
- `HomeViewModel` 根据 query 做本地过滤或远端搜索。
- 添加搜索结果态、空态、清除逻辑。

### 4.4 错误态、空态、加载态不统一

多个 ViewModel 有 `error` 或 effect，但 UI 没展示。例如 Home 的 `error` 没有渲染，Conversation/Friend 的 `ShowError` 分支为空。

建议：

- 在 `core:ui` 增加通用 `LoadingState`、`ErrorState`、`EmptyState`。
- Feature UI 必须覆盖 loading/error/empty/content 四态。
- 错误 effect 用 SnackbarHost 统一展示。

### 4.5 设置页与退出登录未接真实逻辑

位置：`feature/profile-services/src/main/java/com/sakuya/profileservices/ui/SettingsScreen.kt`

设置页大量 TODO，尤其“退出登录”没有清除 token，也没有导航回登录页。

建议：

- Settings 引入 ViewModel。
- 注入 AuthRepository 或 TokenStorage。
- 退出登录后清空 token、清理敏感本地缓存、导航到登录页并清空 back stack。

### 4.6 Reader 阅读器缺少错误态和进度持久化

位置：`feature/reader/src/main/java/com/sakuya/reader/viewmodel/ReaderViewModel.kt`

问题：

- 不支持的文件或打开失败时只显示空内容。
- `saveProgress()`、`restoreProgress()` 为空。
- `uri.path!!` 有潜在 NPE。
- 依赖注入了 `ReaderRepository/FileDownloader`，但当前打开文件流程没用上。

建议：

- `ReaderUiState` 增加 `errorMessage`。
- 使用 DataStore 或 Room 保存 `bookId/filePath -> progress/fontSize`。
- 对 content URI 使用 MIME type 或 DisplayName 判断格式。

## 5. P2 缺陷：中优先级

### 5.1 API 地址与环境配置硬编码

位置：

- `core/data/src/main/java/com/sakuya/data/remote/CoreNetworkModule.kt`
- `feature/conversation/src/main/java/com/sakuya/conversation/data/remote/ChatWebSocket.kt`

建议：

- `BASE_URL`、`WS_URL` 迁移到 BuildConfig。
- 配置 dev/staging/prod productFlavors。
- OkHttp 添加 debug logging interceptor，仅 debug 开启。

### 5.2 Room 迁移策略缺失

位置：`core/di/src/main/java/com/sakuya/di/DatabaseModule.kt`

当前 version=1 且没有 migration。后续加字段或表时，用户升级会遇到 schema mismatch。

建议：

- 开启 `exportSchema = true` 并提交 schema。
- 每次 schema 变更补 migration。
- 早期开发可 debug 使用 destructive migration，但 release 禁用。

### 5.3 测试覆盖薄弱

当前仍以示例测试或少量 reader loader 测试为主，且工作区里 reader 测试文件存在删除状态。

建议优先补：

- AuthViewModel 登录/注册校验和成功失败测试。
- HomeViewModel 加载、错误、搜索测试。
- Conversation/Friend Repository 的 mock/remote 行为测试。
- ReaderViewModel 打开 txt/epub/失败文件测试。
- ProfileMeViewModel 保存资料、头像上传成功失败测试。

### 5.4 代码卫生问题

建议清理：

- 无用 import：如 `androidx.lifecycle.viewmodel.compose.viewModel`、`retrofit2.http.Multipart` 等。
- 注释中的口语化内容移到提交说明或删去，保留必要技术注释。
- `.kotlin/errors/*.log`、`feature/profile-services/build/*` 这类生成物不应进入版本管理。
- `feature/settings` 若长期为空，先移出 settings.gradle，或补最小页面。

## 6. 建议开发路线

### Phase 1：把已露出的用户流程闭环

目标：点击不空白，保存不丢失，错误可见。

- 修复底部导航 `selected`。
- 关闭默认跳过登录。
- 个人资料编辑统一走 ViewModel 保存。
- 设置页实现退出登录。
- 好友添加、通知、群聊先补最小可用页面。
- Home 搜索栏改为受控组件。

### Phase 2：替换 mock 数据源

目标：主要页面具备真实数据来源。

- Home/Friend/Conversation/Library 接入真实 API 或本地 Room。
- mock 数据迁到 debug flavor。
- Repository 统一采用 remote + local cache 策略。
- 增加下拉刷新和重试。

### Phase 3：聊天与好友系统增强

目标：聊天可用、好友申请可用。

- WebSocket 状态机、重连、token 失效处理。
- 消息 pending/sent/failed/read 状态。
- Room 持久化聊天记录和会话摘要。
- 好友搜索、申请、接受、拒绝、删除。

### Phase 4：Reader 和收藏完善

目标：阅读和收藏变成可长期使用的功能。

- Reader 保存阅读进度、字号、最近打开文件。
- Reader 错误态和不支持格式提示。
- Library 接入收藏 API/Room，取消收藏同步后端。
- 从 Home 内容卡片跳转详情页并支持收藏。

### Phase 5：工程化与质量

目标：降低回归风险。

- 增加 ViewModel/Repository 单元测试。
- 增加 Room migration 测试。
- 配置 debug/release/prod 环境。
- 增加 CI：compile、unit test、lint。
- 清理生成文件和空模块。

## 7. 推荐的状态模型

建议各 Feature 统一使用类似结构：

```kotlin
data class ScreenUiState<T>(
    val isLoading: Boolean = false,
    val data: T? = null,
    val errorMessage: String? = null,
    val isEmpty: Boolean = false
)
```

对于一次性事件使用 `SharedFlow<Effect>`：

```kotlin
sealed interface FeatureEffect {
    data class ShowSnackbar(val message: String) : FeatureEffect
    data class Navigate(val route: String) : FeatureEffect
}
```

不要把持久 UI 状态放进 effect，也不要用可变 data class 字段直接在 Composable 中修改。

## 8. 下一步最小任务清单

建议按这个顺序开工：

1. 修复 `MainScreen` 底部导航选中态。
2. 将 `DEV_SKIP_AUTH` 改成可配置，默认不跳过登录。
3. 给 Settings 接入退出登录。
4. 修复 Profile 编辑保存链路。
5. 给 Friend 空路由补页面。
6. Home 搜索栏受控化并接入 ViewModel。
7. WebSocket `CONNECTED` 状态、空 token、防重复连接、重连。
8. Reader 增加错误态和进度保存。
9. 清理生成文件和无用 import。
10. 为 Auth/Profile/Home/Reader 各补 1-2 个关键单测。

