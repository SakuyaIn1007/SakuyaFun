# SakuyaInAndroid Agent Guide

## 1. Project overview

SakuyaInAndroid is a multi-module Android application with a Spring Boot backend.

- Android client: Kotlin, Jetpack Compose, MVVM, Hilt, Coroutines/Flow, Retrofit/OkHttp, Room/DataStore.
- Backend: Java 21, Spring Boot, Spring Security, Spring Data JPA, WebSocket, MySQL/H2.
- Primary packages use `com.sakuya`.
- Android modules are organized as `app`, `core:*`, and `feature:*`; backend code is in `backend/`.

Read `DEVELOPMENT_PLAN.md` and `PROJECT_DEVELOPMENT_REPORT.md` before making architecture-wide changes. They describe the intended module responsibilities and current implementation gaps.

## 2. Module boundaries

- `app`: application entry point, app-level navigation, and composition root only. Do not put feature business logic here.
- `core:model`: cross-feature domain models and result/error abstractions.
- `core:data`: shared network, persistence, authentication, and database infrastructure.
- `core:di`: app-wide dependency-injection bindings.
- `core:ui` and `core:designsystem`: reusable UI primitives, themes, icons, and components only.
- `core:navigation`: shared route contracts and cross-feature navigation abstractions.
- `feature:*`: feature-owned UI, ViewModels, navigation, feature data sources, repositories, and DI modules.
- `backend`: REST/WebSocket APIs, application services, persistence entities, security, and integration tests.

Preserve module dependency direction. Features may depend on relevant `core:*` modules; `core:*` modules must not depend on features. Avoid direct feature-to-feature implementation dependencies; communicate through stable models, route contracts, or an explicitly shared core abstraction.

## 3. Android architecture rules

Keep the Android data flow explicit:

```text
Composable UI -> ViewModel -> Repository -> DataSource / ApiService / DAO
```

- Composables render state and send user events. They must not perform network, database, or long-running business operations.
- Each stateful screen should expose an immutable `UiState`, accept explicit UI events/actions, and keep one-off navigation or messages separate from durable state.
- ViewModels own screen business logic and launch work in `viewModelScope`.
- Repositories own data orchestration and hide Retrofit, Room, DataStore, and file implementation details.
- Use Hilt constructor injection; do not create repositories, Retrofit services, DAOs, or ViewModels manually in UI code.
- Use `StateFlow`/`Flow` for observable state. Do not introduce `GlobalScope`, blocking calls on the main thread, or unbounded coroutine work.
- Run blocking I/O, parsing, database, and file operations on an appropriate dispatcher, normally `Dispatchers.IO`.
- Model loading, content, empty, and error states. Include a retry path for user-recoverable failures.
- Keep navigation routes in the owning feature or `core:navigation`; do not scatter duplicate string routes.

## 4. Jetpack Compose rules

- Prefer state hoisting: pass state down and callbacks up.
- Keep Composables small and focused. Extract independently testable UI sections rather than creating large screen functions.
- Use stable keys for lazy-list items and avoid performing data transformation or I/O during composition.
- Collect flows with lifecycle awareness where the existing project setup supports it.
- Reuse components, colors, typography, and icons from `core:ui` or `core:designsystem` before adding feature-local duplicates.
- Preserve the existing visual language unless a task explicitly changes design.

## 5. Backend rules

Maintain the server flow:

```text
Controller -> Service / application logic -> Repository -> JPA entity
```

- Controllers validate input, map HTTP concerns, and delegate; they do not contain persistence or business workflows.
- Keep security and authorization checks explicit. Never weaken authentication, CORS, JWT, or WebSocket handshake behavior as a shortcut.
- Use DTOs or response models at API boundaries; do not expose JPA entities accidentally.
- Keep API errors consistent with the existing `ApiResponse`, `BusinessException`, and global exception-handling conventions.
- Add or update integration tests when changing authentication, authorization, request validation, persistence behavior, or public endpoints.

## 6. Code quality and comments

All new or materially changed code must include useful documentation in Chinese where it clarifies responsibility, control flow, lifecycle, threading, state ownership, or non-obvious decisions.

For important Kotlin/Java classes (especially ViewModels, repositories, services, controllers, data sources, and complex UI state), start with a responsibility comment in this style and adapt it to the class:

```kotlin
/**
 * HomeViewModel.kt
 * 职责说明：
 * 1. 负责首页课程表、通知、作业等业务逻辑。
 * 2. 统一管理 HomeUiState。
 * 3. 协调数据加载、失败处理与刷新流程。
 */
```

- Explain *why* and the execution flow, not obvious syntax.
- Document public contracts, threading requirements, state transitions, and error behavior when they are not self-evident.
- Do not add boilerplate comments that merely restate a method or variable name.
- Keep names, formatting, nullability, and error handling consistent with nearby code.

## 7. Working process

Before implementing a non-trivial feature, refactor, dependency change, API change, or cross-module change:

1. Inspect the relevant module, similar implementation, dependency graph, and current Git diff.
2. State the implementation plan, files to change, data-flow impact, test plan, and meaningful risks.
3. Wait for confirmation if the task asks for planning/review only, changes public contracts, affects several modules, changes persistence/security, or has unclear product behavior.
4. Implement the smallest coherent change; do not rewrite unrelated code.
5. Run the narrowest relevant checks, then report changed files, verification results, and any remaining risks.

For small, well-scoped bug fixes with unambiguous expected behavior, implementation may proceed directly after inspection.

## 8. Verification

- Do not edit, delete, stage, or revert unrelated user changes. This repository may be dirty.
- Do not commit, push, change secrets, or modify local environment configuration unless explicitly asked.
- Prefer focused Gradle tasks for the affected module, such as `./gradlew :feature:<name>:test`, before a whole-project build.
- Use `./gradlew test` for broader Android/JVM test verification only when appropriate.
- For backend changes, use `./gradlew -p backend test` or the narrowest relevant backend test.
- If a check cannot run, report the exact command and blocker instead of claiming success.

## 9. Change-specific guidance

- Network/API work: update the API service, data source, repository, DI binding, UI state/error handling, and tests as applicable.
- Database work: include entity/DAO/database migrations or schema compatibility considerations; do not silently lose user data.
- Reader work: treat EPUB/TXT parsing and file copy/download as I/O; keep parser/file details out of composables.
- Conversation work: treat WebSocket lifecycle, reconnect, ordering, persistence, and unread state as explicit design concerns.
- Security work: preserve least privilege and validate both authenticated and unauthenticated behavior.
