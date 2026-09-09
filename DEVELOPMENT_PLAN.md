# SakuyaInAndroid 开发规划文档

## 项目总览

| 项目 | 说明 |
|------|------|
| 名称 | SakuyaInAndroid |
| 类型 | Android 原生应用 (Kotlin + Jetpack Compose) |
| 架构 | 多模块 (Multi-Module) + MVI（conversation/feed 模块）+ MVVM（其余模块） |
| DI | Hilt |
| 网络 | Retrofit + OkHttp + Gson |
| 导航 | Jetpack Navigation Compose |
| 图片 | Coil |
| 最低 SDK | 24 (Android 7.0) |
| 目标 SDK | 35 |

---

## 一、项目模块结构

```
sakuyainandroid/
├── app/                          # 主入口 + app 层聚合 UI（DashboardScreen 等跨 feature 编排页）
├── core/
│   ├── common/                   # 公共工具 + MVI 基础设施 (MviViewModel / BaseMviViewModel)
│   ├── data/                     # 网络层基础设施、Room、通知、离线同步 (TokenStorage, AuthInterceptor, DAOs)
│   ├── designsystem/             # 图标资源 (SakuyaIcons)
│   ├── di/                       # 全局 DI 绑定 (DatabaseModule, StorageModule)
│   ├── model/                    # 全部领域/共享模型 (feed、chat、friend、profile、group、notification、search)
│   ├── navigation/               # 跨 feature 的路由常量与 pattern
│   └── ui/                       # UI 组件库 (TopBar, GradientText, Outline, Theme)
├── feature/
│   ├── authentication/           # 登录/注册
│   ├── conversation/             # 会话列表 + 聊天 + 聊天详情 + 记录搜索（MVI）
│   ├── friend/                   # 好友/群组
│   ├── feed/                     # 动态时间线/详情/发布 + 他人主页与关注列表（MVI）
│   ├── dashboard/                # 首页提示状态（聚合 UI 已上移 app）
│   ├── notification/             # 通知页
│   ├── catalog/ bookshelf/ reader/ bookdetail/ search/  # 书库与阅读相关
│   └── profile/ profile-services/ settings/
└── services/wenku8-adapter/      # 轻小说适配服务
```

### 模块依赖规则

1. **feature 之间禁止互相依赖**：跨业务跳转一律经 `core:navigation` 的路由字符串（NavHost 在 app 层装配）。
2. **共享模型只放 `core:model`**：chat/friend/feed 相关的领域模型与公共 DTO 统一下沉（如 `com.sakuya.model.chat`、`com.sakuya.model.profile`），feature 内仅保留自身的私有 DTO 与映射。
3. **core 不反向依赖 feature**；`core:model` 保持纯 Kotlin，无 Android 依赖。
4. **跨 feature 编排 UI 放 app 模块**：需要同时渲染多个 feature 的页面（如首页嵌入动态时间线）上移至 app，避免产生 feature→feature 编译依赖。
5. **MVI 约定（conversation/feed）**：意图统一经 `onAction(Action)` 进入；可重放的页面状态只存单一不可变 `UiState`；Snackbar/导航等一次性事件走 `effect`（Channel 语义）；持久页面条件（如列表错误+重试入口）保留在 UiState。基础设施见 `core:common` 的 `BaseMviViewModel`。

---

## 二、各模块现状分析

### ✅ 已实现较好的部分

| 模块 | 完成度 | 说明 |
|------|--------|------|
| `feature/authentication` | 85% | 登录/注册完整 UI + ViewModel + Repository + ApiService + DI，表单验证完善 |
| `feature/profile` | 75% | 个人主页 + 资料编辑 + 头像上传 + 隐私设置均有实现，ViewModel 和 Repository 完整 |
| `core/ui` | 80% | Theme 色彩体系完善，TopBar/Outline/GradientText 组件封装良好 |
| `core/data` | 70% | TokenStorage、AuthInterceptor、CoreNetworkModule 基础设施完整 |
| `app` | 75% | MainActivity 启动流程、底部导航、登录态判断均实现 |

### ⚠️ 存在明显空缺的模块

| 模块 | 完成度 | 主要问题 |
|------|--------|----------|
| `feature/home` | 40% | 无 ViewModel，全部数据硬编码在 Composable 内 |
| `feature/library` | 45% | ViewModel 存在但使用静态 Mock 数据，无真实数据源 |
| `feature/conversation` | 35% | 会话列表和聊天界面均为纯 UI，无任何 ViewModel 或数据层 |
| `feature/friend` | 30% | 仅有好友列表 UI，无 ViewModel，无数据层，好友申请/添加流程缺失 |
| `feature/profile-services` | 40% | 设置页 UI 完整但全是 TODO，钱包/收藏/卡包均为骨架页面 |
| `core/navigation` | 0% | 空模块，无任何代码 |
| `feature/settings` | 0% | 空模块，无任何代码 |

---

## 三、急需解决的核心问题

### 🔴 P0 - 阻塞性问题

#### 1. 数据层缺失：绝大部分功能无真实数据源

几乎所有 Feature 模块的 UI 都使用硬编码或 Mock 数据，**无法对接后端服务**。

**需解决的问题：**
- [x] `feature/authentication` — AuthApiService + AuthRepository 已就绪
- [x] `feature/profile` — ProfileApiService + UserRepository 已就绪
- [ ] `feature/home` — 需要 HomeApiService + HomeRepository
- [ ] `feature/library` — 需要 LibraryApiService + LibraryRepository
- [ ] `feature/conversation` — 需要 ConversationApiService + ChatRepository
- [ ] `feature/friend` — 需要 FriendApiService + FriendRepository

**实现方式参考** `AuthRepository` 的结构（位于 [feature/authentication/data/repository/AuthRepository.kt](file:///Users/xiaoye/FlutterProjects/sakuyainandroid/feature/authentication/src/main/java/com/sakuya/authentication/data/repository/AuthRepository.kt)）：

```
data/remote/ApiService.kt  → 定义 Retrofit 接口
di/NetworkModule.kt         → 提供 ApiService 实例 (Hilt)
data/repository/Repository.kt → 封装数据访问逻辑
```

#### 2. ViewModel 普遍缺失

以下模块完全没有 ViewModel，导致页面"纯展示"，无任何交互逻辑：

- `feature/conversation` — ConversationScreen / ChatScreen 均无 VM
- `feature/friend` — FriendScreen 无 VM
- `feature/home` — HomeScreen / MusicScreen / NovelScreen 无 VM
- `feature/profile-services` — 所有子页面无 VM

#### 3. `core/navigation` 模块完全空置

该模块本应用于存放**跨模块共享的导航路由常量**和**通用导航逻辑**，但目前 Route 定义散落在各个 Feature 模块中，导致：

- 路由常量重复定义风险
- 跨模块导航依赖混乱
- 无法统一管理 Deep Link

#### 4. API Base URL 硬编码

`CoreNetworkModule` 中 [baseUrl](file:///Users/xiaoye/FlutterProjects/sakuyainandroid/core/data/src/main/java/com/sakuya/data/remote/CoreNetworkModule.kt#L32) 硬编码为 `https://api.sakuya.com/`，应改为从 BuildConfig/配置文件中读取，以支持多环境切换（dev/staging/prod）。

---

### 🟡 P1 - 高优先级

#### 5. 聊天功能严重不完整

[ChatScreen](file:///Users/xiaoye/FlutterProjects/sakuyainandroid/feature/conversation/src/main/java/com/sakuya/conversation/ui/ChatScreen.kt) 目前是纯本地 UI Demo，缺失：

- 消息发送（已有输入框 UI，但发送逻辑仅本地追加 list）
- 消息接收（无 WebSocket/轮询机制）
- 消息持久化（无 Room 数据库）
- 图片/文件消息类型
- 消息已读/未读状态
- 好友资料卡片跳转

#### 6. 好友系统缺失

Friend 模块目前仅有一个展示列表的 UI，缺失完整的好友系统：

- 添加好友（搜索用户、发送申请、同意/拒绝申请）
- 好友请求通知
- 好友搜索
- 群聊入口
- 好友在线状态实时更新

#### 7. 首页缺乏数据驱动

[HomeScreen](file:///Users/xiaoye/FlutterProjects/sakuyainandroid/feature/home/src/main/java/com/sakuya/home/ui/HomeScreen.kt#L177-L188) 的推荐列表完全硬编码，Banner 轮播也是静态数据。需要：

- HomeViewModel 管理状态
- 推荐算法/后端接口接入
- 下拉刷新
- 分页加载
- 点击内容跳转详情页

#### 8. 无本地持久化数据库

项目目前没有使用 Room 数据库，仅通过 DataStore/SharedPreferences 存储 Token。这导致：

- 离线状态下所有列表为空
- 每次打开 App 都需要重新请求数据
- 聊天记录无法本地存储

---

### 🟢 P2 - 中优先级

#### 9. 测试完全空白

所有模块的 `test/` 和 `androidTest/` 目录中仅包含示例骨架文件，无任何实际测试。

**需要补全的测试：**

| 类型 | 优先级 | 覆盖范围 |
|------|--------|----------|
| ViewModel 单元测试 | 高 | AuthViewModel, ProfileViewModel, LibraryViewModel 等 |
| Repository 单元测试 | 高 | AuthRepository, UserRepository |
| API Service 单元测试 | 中 | MockWebServer 验证请求体 |
| UI 组件测试 | 中 | 关键 Composable 的 render 验证 |
| 端到端测试 | 低 | 登录→首页→聊天 主流程 |

#### 10. 异常处理不完善

当前：
- `BaseResponse.toResult()` 只返回 success/failure
- 无全局异常处理器（如统一的 Snackbar 展示网络错误）
- 无重试机制
- 无加载失败后的"重新加载"UI

#### 11. 缺少空状态和错误状态 UI

多个列表页面在数据为空时仅显示简单文字提示，缺少：
- 图文空状态（插图 + 引导文字 + 操作按钮）
- Error/Retry 状态 UI
- Loading Skeleton 骨架屏

#### 12. Gradle 配置待优化

- 缺少 `libs.versions.toml` 版本目录文件，依赖版本散落在各 `build.gradle` 中
- ProGuard/R8 混淆规则为空（`proguard-rules.pro`）
- 缺少 CI/CD 配置
- Release 构建未开启代码混淆和资源压缩

---

## 四、功能模块扩充路线图

### Phase 1：基础设施补齐（建议第一优先级）

```
预计工作量：重要，1-2周
目标：让项目具备真实开发条件
```

| 编号 | 任务 | 涉及模块 |
|------|------|----------|
| 1.1 | 创建 `libs.versions.toml` 统一管理依赖版本 | 根目录 |
| 1.2 | 将 `baseUrl` 改为 BuildConfig 配置，支持 dev/staging/prod 三环境 | `core/data` |
| 1.3 | 添加 Room 数据库依赖，创建基础 AppDatabase | `core/data` |
| 1.4 | 实现 TokenStorageImpl 的 DataStore 本地持久化 | `core/data` |
| 1.5 | 整理 `core/navigation` 模块，迁移所有 Route 常量到此 | `core/navigation` |
| 1.6 | 创建全局异常处理工具（NetworkResult sealed class） | `core/model` |

### Phase 2：数据层打通（建议第二优先级）

```
预计工作量：重要，2-3周
目标：所有主要 Feature 具备真实数据交互能力
```

| 编号 | 任务 | 涉及模块 |
|------|------|----------|
| 2.1 | 实现 HomeApiService + HomeRepository，对接首页推荐接口 | `feature/home` |
| 2.2 | 创建 HomeViewModel，替换硬编码数据 | `feature/home` |
| 2.3 | 实现 LibraryApiService + LibraryRepository，对接收藏接口 | `feature/library` |
| 2.4 | 加强 LibraryViewModel，支持真实增删改查 | `feature/library` |
| 2.5 | 实现 ConversationApiService + ConversationRepository | `feature/conversation` |
| 2.6 | 创建 ConversationViewModel + ChatViewModel | `feature/conversation` |
| 2.7 | 实现 FriendApiService + FriendRepository | `feature/friend` |
| 2.8 | 创建 FriendViewModel，支持好友列表/搜索/申请 | `feature/friend` |

### Phase 3：功能完善（建议第三优先级）

```
预计工作量：中等，2-4周
目标：各功能模块达到可用水平
```

| 编号 | 任务 | 涉及模块 |
|------|------|----------|
| 3.1 | 首页下拉刷新 + 分页加载 + 点击跳转详情 | `feature/home` |
| 3.2 | 聊天功能完善：消息发送 API、消息接收（WebSocket）、本地存储 | `feature/conversation` |
| 3.3 | 好友系统完整流程：搜索 → 申请 → 同意 → 列表展示 | `feature/friend` |
| 3.4 | 设置页功能接入：退出登录、切换账号、消息通知开关等 | `feature/profile-services` |
| 3.5 | 钱包/卡包/收藏页面的真实功能实现 | `feature/profile-services` |
| 3.6 | 相册模块完善：图片上传/查看原图/删除 | `feature/profile-services` |
| 3.7 | 忘记密码流程 | `feature/authentication` |

### Phase 4：体验优化 + 测试（建议第四优先级）

```
预计工作量：中等，2-3周
目标：完善测试覆盖和用户体验细节
```

| 编号 | 任务 | 涉及模块 |
|------|------|----------|
| 4.1 | 全局 Loading Skeleton 骨架屏组件 | `core/ui` |
| 4.2 | 空状态组件（含插图和操作引导） | `core/ui` |
| 4.3 | 全局 Error/Retry UI 组件 | `core/ui` |
| 4.4 | 编写 AuthViewModel 单元测试 | `feature/authentication` |
| 4.5 | 编写 ProfileViewModel 单元测试 | `feature/profile` |
| 4.6 | 编写 Repository 层单元测试 | 各 Feature 模块 |
| 4.7 | 编写关键 Composable UI 测试 | 各 Feature 模块 |
| 4.8 | 开启 Release 构建混淆 + 资源压缩 | 各模块 build.gradle |

---

## 五、代码质量待改进项

### 5.1 编程惯例改进

1. **Profile 模块使用 var 直接修改属性**
   - [ProfileNavigation.kt](file:///Users/xiaoye/FlutterProjects/sakuyainandroid/feature/profile/src/main/java/com/sakuya/profile/navigation/ProfileNavigation.kt#L209-L210) 中 `profile.gender = newGender` / `profile.phoneNumber = it` 直接修改 data class 属性，应使用 `copy()` 方法保持不可变性。

2. **Model 类分散放置**
   - `Conversation` 和 `Friend` 的数据模型定义在各自 Feature 模块内，如果其他模块需要引用会产生循环依赖。建议移到 `core/model`。

3. **ToplevelNavItem 导航栏颜色常量硬编码**
   - 底部导航栏的颜色使用 `.inverseOnSurface` 而非独立语义化的命名，不利于后续主题扩展。

### 5.2 安全检查项

1. **Token 存储** — 确认 TokenStorageImpl 使用了 EncryptedSharedPreferences 或 DataStore 加密存储，而非明文存储 Token。

2. **网络安全** — 当前 OkHttpClient 未配置 Certificate Pinner 或网络安全策略，Release 时建议添加。

---

## 六、总结

| 类别 | 已完成 | 待完成 | 完成率 |
|------|--------|--------|--------|
| 项目基础设施 | 7 | 5 | ~58% |
| ViewModel 覆盖 | 3/9 | 6 | 33% |
| 数据层 (API+Repo) | 2/7 | 5 | 28% |
| 核心业务流程 | 2/6 | 4 | 33% |
| UI 组件库 | 8 | 3 | ~73% |
| 测试 | 0 | 全部 | 0% |

**当前项目处于**：UI 框架基本搭建完成，但数据层和业务逻辑层大面积缺失的早期阶段。建议优先完成 Phase 1（基础设施）和 Phase 2（数据层打通），让项目具备端到端的完整数据链路，再逐步完善功能细节和测试。
