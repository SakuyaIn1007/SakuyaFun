# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> `AGENTS.md` is the repository's own agent guide and covers module boundaries, per-layer rules, and the working process in more detail. Read it before architecture-wide changes; this file focuses on commands and cross-module invariants that are hard to discover from a single file.

## What this repo is

Two deployables in one Gradle tree:

- **Android client** — Kotlin, Jetpack Compose, MVVM, Hilt, Coroutines/Flow, Retrofit/OkHttp, Room. Modules: `app`, `core:*`, `feature:*`.
- **Spring Boot backend** (`backend/`) — Java 21, Spring Web/Security/Data JPA/WebSocket, MySQL or H2. Has its **own** Gradle build (`backend/settings.gradle`), so it is always driven with `-p backend` from the repo root.

Root `settings.gradle` lists all Android modules. Namespace ≠ module name in two places: `feature:bookshelf` uses package `com.sakuya.library`, and `feature:profile-services` uses `com.sakuya.profileservices`.

## Commands

### Android

```bash
./gradlew :feature:reader:compileDebugKotlin          # narrowest compile check
./gradlew :feature:feed:testDebugUnitTest             # unit tests for one module
./gradlew :feature:feed:testDebugUnitTest --tests "com.sakuya.feed.SomeTest"
./gradlew assembleDebug                               # full assemble
./gradlew test                                        # all JVM unit tests, all modules
```

Unit-test tasks are `testDebugUnitTest` (per module), not `test`. Debug builds take API endpoints from Gradle properties, so point the client at a locally running backend without editing source:

```bash
./gradlew :app:installDebug \
  -PapiBaseUrl=http://10.0.2.2:8080/ \
  -PwsChatUrl=ws://10.0.2.2:8080/ws/chat
```

`10.0.2.2` is the emulator's alias for the host. `-PdevSkipAuth=true` sets `BuildConfig.DEV_SKIP_AUTH` in debug. Defaults live in `core/data/build.gradle` (`10.0.2.2` for debug, `api.sakuya.com` for release). The Google Services plugin is applied **only if** `app/google-services.json` exists — never commit it.

### Backend

```bash
./gradlew -p backend test                              # all backend tests
./gradlew -p backend test --tests "com.sakuya.backend.content.ContentApiIntegrationTest"
./gradlew -p backend bootRun                           # default dev profile, MySQL `sakuya` on localhost
docker compose -f backend/docker-compose.yml up -d     # MySQL 8.4 (root/password)
```

Dev profile seeds demo accounts — `sakuya`, `alice`, `bob`, `rin` plus six more (`qianxia`, `yuzhen`, `ling`, `che`, `mingli`, `lian`) — all with password `password123`. Non-dev profiles require `JWT_SECRET` and create no demo data.

Seed content lives in `backend/src/main/resources/seed/demo-data.yml`, not in Java: accounts, friendships, follows, friend requests, library entries, conversations, posts, comments and likes are all declared there and imported by `config/seed/DemoSeedWriter` under `app.seed.enabled=true`. Adding demo content means editing that YAML only. Import is **idempotent per row** via natural keys (feed posts key on author+title), so restarting never duplicates rows — and, unlike the old `posts.count() > 0` guard it replaced, never silently skips the whole import just because the database already had a post. `SeedDataLoader` validates references before writing and fails startup with a precise message on a bad account name or out-of-range `replyTo`.

Several demo counts are pinned by `AuthApiIntegrationTest#sakuyaAccountHasCompleteDemoData` (friends, library size, conversation count) and by the `$.data[0].name == "凛"` assertion in the same test — adjust those together with the YAML.

### Wenku8 adapter (Python, `services/wenku8-adapter`)

An internal FastAPI service wrapping `pywenku8api`; Spring Boot calls it over the internal network, the Android client never does. It is **off by default on both ends** — `WENKU8_ADAPTER_ENABLED=false` server-side and `WENKU8_ENABLED` unset adapter-side — so nothing scrapes upstream by accident.

```bash
./backend/scripts/run-wenku8-local.sh                  # adapter -> /health?verify=true -> backend; asserts 503 ADAPTER_DISABLED
WENKU8_ENABLED=true WENKU8_ADAPTER_ENABLED=true \
  WENKU8_USERNAME=... WENKU8_PASSWORD=... ./backend/scripts/run-wenku8-local.sh
```

The script prefers `services/wenku8-adapter/.venv/bin/python`; install `requirements.txt` there first. Credentials are env-only and must never be written to a file. Adapter tests run standalone (pytest is intentionally not a dependency):

```bash
cd services/wenku8-adapter && .venv/bin/python tests/test_throttle.py
```

## Hard contracts — `docs/` (read before changing state, interfaces, or invariants)

Three documents under `docs/` hold this repo's **硬约定** (hard conventions). They are normative, not descriptive: when code and a document disagree, the code wins — fix the document immediately. Each cites `file:line` and names the tests that guard it.

| Document | Read it when | Covers |
| --- | --- | --- |
| [`docs/state-machine.md`](docs/state-machine.md) | Adding/removing a status value, or changing a transition | All state enums and their legal transitions, triggers, terminal states, concurrency guards | 
| [`docs/contracts.md`](docs/contracts.md) | Changing an endpoint, response shape, provider protocol, route, or DAO query | HTTP envelopes and error mapping, `ContentProvider` protocol and provider capability boundaries, navigation route constants, Room/`ownerId` isolation, bookId namespaces, auth rules |
| [`docs/constraints.md`](docs/constraints.md) | Touching anything in the content/import/cache/offline path | Invariants whose violation causes silent corruption — offset contract, two-phase import, empty-chapter handling, idempotence, cache isolation, credential hygiene. Includes a constraint→test table marking which rules have **no** automated protection |

**The three that bite hardest**, each from a real incident recorded in the comments:

1. **Character-offset contract.** `ContentImportService` builds full text as `volumeTitle + ' ' + chapterTitle + '\n'`, and `ChapterSplitter` emits offsets against exactly that. Breaking it doesn't throw — it silently jumps readers to the wrong passage.
2. **Cache isolation.** HTTP caching lives only on the `@ContentCacheClient` OkHttpClient. OkHttp cache keys are URL-only with no `Vary`, so caching on the shared client leaks personal data across account switches. No test guards this.
3. **bookId namespace.** Both `WENKU8` and `WENKU8_CDN` must import to `wenku8-{aid}`, never `content-{UUID}` — otherwise the id the user opened can never read the imported content.

**Committing these docs:** `docs/` matches the `/docs` rule in `.gitignore`, and that is intentional — this repo's convention is to add docs explicitly rather than change the ignore rule:

```bash
git add -f docs/state-machine.md docs/contracts.md docs/constraints.md
```

`git add` without `-f` skips them **silently** (no error), so an edited document can go uncommitted indefinitely. The existing `docs/superpowers/**` files entered the repo the same way.

## Architecture

### Client data flow and navigation

`Composable -> ViewModel -> Repository -> DataSource / ApiService / DAO`. `app` is composition root only — it wires nav graphs and holds no feature logic.

Navigation is split deliberately: **route string constants live in `core:navigation`** (`*Routes.kt`, e.g. `ReaderRoutes.kt`), each feature exposes a `NavGraphBuilder.<x>NavGraph(navController)` in its own `navigation/` package, and `app/.../navigation/MainNavGraph.kt` composes them. Adding a screen means touching a route contract, the feature nav graph, and `MainNavGraph` — not adding a literal string at a call site. Feature-to-feature calls go through these route contracts, never direct module dependencies.

### Backend content subsystem (largest cross-cutting area)

Content read path is an explicit two-stage migration:

```
/content/**  ->  ContentFallbackFacade  ->  ContentReadService (DB)
                                        ->  DownloadSourceContentProvider (CDN)   [preferred fallback]
                                        ->  Wenku8GatewayService (adapter)        [last resort]
```

`ContentManagementProperties.readMode` (`CONTENT_READ_MODE`) selects the strategy: `DB_ONLY` makes every DB miss a hard 404, `DB_FIRST_WITH_ADAPTER_FALLBACK` enables the chain. Fallback fires **only** on 404 and only for non-`content-` book ids. The CDN branch is tried first because it does not go through Cloudflare; it must return content **synchronously** since `imports.backfill()` is `@Async` (reading back from DB right after backfill would still 404), with the async import fired anyway so the next read hits the DB.

Ingestion is provider-based: `ContentProvider` (`id` / `catalog` / `book` / `volumes` / `chapterContent` / `cover`) is implemented by `Wenku8ContentProvider` (adapter, supports catalog enumeration) and `DownloadSourceContentProvider` (CDN, per-book only — its `catalog()` throws `UnsupportedOperationException`). `ContentImportRunner` indexes providers by upper-cased id, so `"WENKU8"` and `"WENKU8_CDN"` are independent sources.

**The offset contract.** `ContentImportService` builds the full text as `volumeTitle + ' ' + chapterTitle + '\n' + body`, and chapter anchors are character offsets into exactly that string. `ChapterSplitter`'s offsets target the same "volume + space + title" prefix. Any change to one side silently breaks chapter jumps; the round-trip is pinned by tests in `ChapterSplitterTest`.

Other invariants in that path, each learned from a production incident:

- Import is two-phase — fetch all bodies into memory first, *then* write storage and DB. Storage writes can't join the DB transaction, so interleaving them leaves orphaned revisions.
- Empty chapters are **skipped**, not fatal. An image-only「插图」chapter used to abort the whole import, leaving a `published=true` shell book with zero chapters that 404'd forever. `DownloadSourceContentProvider.hasNoReadableText` and `ContentImportService` filter with the same rule.
- Re-import short-circuits on unchanged `fullSha256` — the catalog sync job re-imports every book daily and would otherwise rewrite storage endlessly.
- `books.cover_byte_size` / `full_content_byte_size` are Java `long` but were historically built nullable; a `NULL` makes `/library`, `/home/**`, and `/books/{id}` all 500. Run `backend/scripts/fix-book-byte-size-columns.sql` after upgrading an existing DB (`ddl-auto=update` never tightens columns). `null`-related 500s across those endpoints are usually this.

### Client content cache and offline

The OkHttp cache lives on a **separate client** (`@ContentCacheClient` in `core/data/.../content/ContentNetworkModule.kt`), never the shared one. OkHttp cache keys are URL-only and the backend sends no `Vary`, so caching on the shared client would persist other users' chat/notifications and leak across account switches. The content client is a whitelist by construction: only `ContentApiService` uses it.

Offline reading (`core/data/.../offline/`) persists a queue in Room and resumes on app start, on enqueue, and on network regain via `OfflineDownloadCoordinator`. Files are written atomically to app-private storage and indexed in Room; deletion removes files before indexes. Capacity eviction must notify the user (`OfflineEvictionEvents`) — silently deleting user-requested downloads is treated as a bug. Reading progress and bookmarks are out of scope for eviction.

Room `AppDatabase` is at **version 15**, `exportSchema = false`, and every version step from 5 to 15 has a registered `Migration` in `core/di/.../DatabaseModule.kt`. Bumping the version without adding the corresponding migration loses user data in release builds (`fallbackToDestructiveMigration` is debug-only, which is exactly why the gap stays invisible during development). Treat schema changes as requiring an explicit migration plan, and verify the migration is actually passed to `addMigrations` — defining one without registering it is a silent no-op.

## Conventions

- **Comments and docs are in Chinese.** Non-trivial classes open with a `职责说明` / `执行流程` block stating responsibility, then control flow. Comment *why* and non-obvious decisions (the offset contract, why the cache is on a separate client), not syntax. Existing code follows this consistently — match it.
- **Don't touch unrelated user changes.** This repo is frequently dirty; never stash, revert, stage, or reformat outside the task.
- Don't commit, push, or change secrets/credentials unless asked. `.gitignore` deliberately excludes `content-store/` (imported full text is copyright-restricted), `.env*`, keystores, Firebase service accounts, and logs.
- API responses are uniform `{"code":200,"message":"success","data":{}}`, aligned with the client's `BaseResponse<T>`. Backend admin writes need both JWT and `X-Content-Management-Token`.
- Design specs and implementation plans for larger features live in `docs/superpowers/specs/` and `docs/superpowers/plans/`; check there before reworking a subsystem.
