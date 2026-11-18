# AGENTS.md

Guidance for AI agents working in this repository.

## Overview

**point** (聚·point) — a BBS/forum system. Design philosophy: 黑墨暖纸 (Black Ink · Warm Paper) — a quiet reading-room aesthetic where vermilion `#c43d3d` is the sole accent, used sparingly.

Two projects share this repo:

| Directory | Stack | Purpose |
|---|---|---|
| `point-frontend/` | Vue 3 + Vuetify + Vite + TypeScript | SPA frontend |
| `point-*/` (7 Maven modules) | Java (JDK 25) + freeway 2.0.1-SNAPSHOT + Maven | REST API backend |

Backend module chain: `point-domain` (entities/DTOs) → `point-db` (repos, schema) → `point-cache` → `point-service` (business logic) → `point-web` + `point-admin` (HTTP routes) → `point-boot` (launcher: `PointApp`, `PointCloudApp`, seeds, cloud export).

## Build & run

### Backend

```bash
mvn -o clean test                        # all tests (24: service 22 + boot 2)
mvn -o clean package -pl point-boot -am  # fat JAR (shade, mainClass = PointApp)
java -jar point-boot/target/point-boot-*.jar   # :8082, H2 at ./data/bbs

./point.sh build [--frontend] [--fast]   # one-command build
./point.sh start|stop|restart|status|logs
```

Single-machine operations (PID file, logs, health check, profiles) are documented in `DEPLOYMENT.md`; `point.sh` defaults to the `dev` profile on :8082.

Use `clean` — an incremental build reuses stale `target/classes` and hides core ABI changes. `-o` (offline) works once the freeway 2.0.1-SNAPSHOT artifacts are in `~/.m2`; drop it when they are not.

`PointCloudApp` is the cloud shape (`CloudModule` + framework `StaticModule` + `RpcExport` of `/rpc/auth/validateToken`); it runs from the same fat JAR via `-cp` and `com.jujin.point.boot.PointCloudApp`, with a profile that supplies `F2.cloud.discovery.static.services` (`dev`/`prod` do), e.g. `--F2.profile=dev`.

### Frontend

```bash
cd point-frontend
npm run dev      # Vite :3000, /api proxied to :8082
npm run build    # vue-tsc --noEmit + production build
npm run lint     # ESLint (flat config); npm run format:check for Prettier
npm run preview  # preview production build
```

`@/` maps to `src/` (Vite alias + tsconfig paths). `tools/sync-frontend.sh` builds and copies `dist/` into `point-boot/src/main/resources/static/`.

## Configuration

JSON, layered: `point-boot/src/main/resources/application.json`, overridden per profile by `application-dev.json` / `application-prod.json`. Framework keys use the uppercase `F2` alias; freeway normalizes `F2.*` to `freeway.*` before merging and unknown-key checks. No schema mode is configured — `freeway.db.schema.mode` defaults to `auto` (entity-driven DDL, no migration files; `validate` would fail a fresh DB).

| Key | Meaning |
|---|---|
| `F2.http.server.port` | HTTP port (8082) |
| `F2.db.url` | JDBC URL — dev H2 `MODE=PostgreSQL`, prod PostgreSQL |
| `bbs.jwt.secret` | JWT HMAC key (fail-fast on the placeholder in prod) |
| `F2.cloud.discovery.static.services` | Cloud-shape static discovery: `serviceId=scheme://host:port[/basePath]` entries; prod names the Kubernetes Service VIP |
| `bbs.jwt.cookie-domain` | Cookie `Domain` for the session cookie (dev: `localhost` for the Vite proxy) |
| `bbs.upload.dir` / `bbs.upload.max-size` | Upload directory / size cap (10 MB) |
| `bbs.admin.username` / `bbs.admin.password` | Config-driven admin seed (no-op when blank) |

## Backend architecture (freeway)

The backend runs on **freeway** (`/Users/apple/Projects/freeway-framework`): IoC container, built-in HTTP engine, JDBC layer, module system. Concepts that matter here:

- **ModuleEx** — composed explicitly with `FreewayApp.create(PointModules.base())` (SPI auto-discovery is off). `PointModule` wires routes, filters, seeds.
- **RuntimeHook** — ordered lifecycle (`start`/`stop`); used for schema migration → data seeding → HTTP server.
- **Container** — constructor injection; routes, filters and event subscribers are contributed, not scanned.
- **HTTP routes** — `binder.contribute(RouteGroup.class).add(XxxRoutes.routes())` in `WebModule` (point-web) / `AdminModule` (point-admin); handlers are container-managed classes.
- **DB** — `Schema.ensure(db, ALL_ENTITIES)` at startup (idempotent); `db.query()` / `db.execute()`; dialect derived from the JDBC URL.

**Auth:** HttpOnly `point_token` cookie (SameSite=Lax → CSRF-safe: all mutations are POST). Legacy `Authorization: Bearer` and `?token=` are still accepted. `AuthFilter` resolves identity; `LoginRateLimiter` throttles signup/signin (10 per 10 min per IP); permission codes cached 60s and invalidated on role changes. `AuthApi` is the in-process seam (`ScopedValue`-based) — unchanged in the cloud shape; peers ride RPC instead.

## Frontend (Vue 3 SPA)

- **State:** Pinia (`src/stores/auth.ts`). Session auth is cookie-based — the store holds the current user only (`src/api/client.ts` sends no token).
- **HTTP:** axios instance, `/api` base, `{ code, message, data }` success envelope, 401 → login redirect. freeway 2.0 maps thrown failures to `{ error, code }`; explicit handler rejections keep the app envelope. The interceptor copies `error` onto `message` so existing views keep one field to read.
- **Routing:** Vue Router, single `DefaultLayout.vue` shell, lazy pages; breakpoints 1300/1200/1100/900.
- **Layout convention:** `[main content, flex 1, max-width 680] | [PageAside, 366px]`; aside hidden below 1100px; `border-right` on main.
- **Themes:** two paper themes via CSS variables + `useTheme()` + `<html class="dark">` — `cicada` (default) and `night`; persisted in `localStorage.paperTheme`.
- **Design tokens:** `--paper-bg/nav/text/text2/border`, accent `--paper-accent` (#c43d3d), hover #a83434. Cards: no shadow, 1px border, 8px radius. Buttons: lowercase, 6px radius. Article body: 17px, line-height 1.9, 680px. Full spec: `point-frontend/DESIGN.md`.
- **Image grid — DO NOT SIMPLIFY:** `src/utils/markdown.ts` `renderMarkdown()` splits content into text segments (markdown-it) and consecutive image groups rendered as `<div class="img-grid cols-N">`. It is the only approach that works with `breaks:true` + XSS-safe `html:false`; simpler alternatives were tried and rejected.

## Testing & CI

- Unit tests live beside code (`*Test`); `mvn -o clean test` = 21 tests.
- End-to-end: `bash tools/smoke-api.sh [base-url]` (17 checks against a running instance).
- CI: `.github/workflows/ci.yml` — JDK 25 + Node 22, backend tests, frontend build/lint, smoke.
- Verification loop for backend changes: `mvn -o clean test` + `mvn -o clean package -pl point-boot -am` + boot the JAR and exercise the changed routes (or `tools/smoke-api.sh`).

## Conventions & pitfalls

- **Let the JDBC URL choose the dialect** — 2.0's H2 dialect is mode-aware and no longer inherits Postgres quoting; an explicit `freeway.db.dialect=h2` means native H2 and is rarely what a `MODE=PostgreSQL` database wants.
- **H2 runs in PostgreSQL mode** (`MODE=PostgreSQL`); keep `NON_KEYWORDS=...` for legacy MySQL-mode data files (identifiers were stored upper-cased; avoid quoted identifiers).
- **`LIKE ... ESCAPE`**: use `ESCAPE '!'` with `!`-escaping — `ESCAPE '\'` breaks under the MySQL-dialect lexer (`\'` swallows `$limit/$offset`).
- **JWT secret**: blank/placeholder → fail-fast in prod, loud warning in dev.
- **Security headers** come from `SecurityHeadersFilter` (CSP self-only, X-Frame-Options DENY, nosniff, Referrer-Policy), ordered before `SpaFilter`.
- **Sessions**: cookie is HttpOnly; `signout` clears it. Don't reintroduce localStorage token storage.
- **API shape**: validated typed bodies (`WebUtils.validatedBody` / `ctx.bodyAsJson`), errors as 4xx (bad params → 400), list endpoints return `{items, page, pageSize, total}`.
- **Frontend deploy**: static files served with `max-age=0` + ETag so redeploys take effect on reload.
- **Event subscribers must not be composition-time `EventSubscriber` contributions** when their factory resolves a service that injects `EventBus` (e.g. `NotificationHandler` → `MessageService`): the bus snapshots its subscription table at construction, and the re-entrant resolution leaves it empty — every notification silently dies. Register from a `RuntimeHook.start` with `bus.subscribeEvent(...)` (see `ServiceModule`). The bus isolates subscriber failures, so no extra try/catch is needed.
- **Deferred contributions are constructed before extension stores seal** (`addDeferred` filters, subscribers). Do not inject `EventBus` — or a service that injects it — into a filter constructor: `EventBus.create` reads the still-publishing subscriber store and startup fails. `AuthFilter` holds the `Container` and resolves its service graph per request; follow that pattern for other contributed filters.
- **Error output goes through `ErrorMapper`** — the server is the only writer; a mapper returns `Optional<ErrorResponse>` and never writes. `PointErrorMapper` maps point's `AuthException`/`ServiceException`/`ValidationException`; framework request rejections fall through to the built-in mapper. Don't reintroduce a writing filter: 2.0 maps exceptions inside each filter level, so an outer filter never sees them.
- **Cloud shape uses framework static discovery** — `PointCloudApp` installs `new StaticModule()`, which reads `F2.cloud.discovery.static.services` (for example `point=http://point.default.svc.cluster.local:8080`). No registration or heartbeat: the platform Service owns membership. Fleet/Consul live in freeway-ext and would replace it with `MemberModule` + the adapter.
- **`bbs_article` has no `last_comment_time` column** (only topics do) — never touch it from the shared comment path; `CommentService.touchLastComment` already guards this after the 2026-11-02 audit.
- **freeway version bumps**: after changing `freeway.version`, run a **clean** build; update the banner in `PointApp` and record it below.

## Version history

- **2.0.1-SNAPSHOT** (2026-11-18) — `StaticModule` is now config-driven: `PointCloudApp` composes `new StaticModule()` and reads `F2.cloud.discovery.static.services`; the interim app-side `PointStaticDiscoveryModule` was removed. 2.0.1's member naming (`MemberModule`, `freeway.cloud.member.*`) does not touch point; the framework-key alias is adopted as uppercase `F2` across the JSON config files. 24/24 tests + 17/17 API smoke green. Banner: `freeway 2.0.1-SNAPSHOT`.

- **2.0.0** (2026-11-17) — adapted to freeway 2.0.0: `HttpContext` → `HttpExchange`, `Query.param` → `bind`, `Row` accessors (`asLong`/`asString`/…), `EventBus.publishEvent`/`subscribeEvent` + `EventSubscriber.ofEvent`, `Contribution.add(Class)`/factory → `addDeferred`, `Endpoint` → `ServiceEndpoint`, `HealthCheck` returns `HealthResult`, `HttpResponse.output` → `body`, `StaticResourceMount.withFallthrough`/`withCacheMaxAgeSeconds` → `withRouteFallback`/`withCacheMaxAge(Duration)`. `ErrorHandler` is gone in favour of `ErrorMapper` (`PointErrorMapper`); error bodies are freeway's `{error, code}` and the SPA normalizes `error` → `message`. `CloudModule` no longer installs discovery; the cloud shape uses the framework's config-driven `StaticModule` from 2.0.1. `AuthFilter` resolves services per request to respect the new construct/publish/seal lifecycle. 24/24 tests + 17/17 API smoke green.

- **Data-logic audit** (2026-11-02) — fixed the event-subscription cycle that killed all site notifications, article-comment 500, soft-deleted-content leaks (comments readable/creatable on deleted topics/articles), cross-entity quote references, ghost categories, ghost likes/favorites, missing article view counter, quote-count rollback and the view-throttle memory leak.

- **1.5.7** (2026-11-02) — release artifact (Maven Central). No code changes: 1.5.7's breaking surface (AccessLogFilter removed, `HttpExchangeEvent.remoteAddress/userAgent`, `setPathVars` replace semantics, `Sql` clause fields, `ON CONFLICT` ordering) does not touch point-bbs. 21/21 tests + API smoke green. Banner: `freeway 1.5.7`.
- **1.5.6-SNAPSHOT** (2026-09-29) — module composition flattened (`ModuleEx[]` + `FreewayApp.create`), config key tables namespaced (`HttpModule.ConfigKeys.*`), seed hooks explicitly ordered.
- **1.5.1** (2026-09-06) / **1.5.0** (2026-09-05) — hardening only / `Binding.to(instance)` → `.to(container -> instance)`.
- **1.4.0** (2026-09-03) — `ExecuteResult.hasKey()` → `hasGeneratedKey()`; `AppConfig.get()` → `snapshot()`.
- **1.3.8** (2026-08-14) — `Row.longValue()`, HTTP API renames (`setHeader`, `setStatus`, `ErrorHandler`), `freeway.web.*` → `freeway.http.*`.
- TODO list: `TODO.md`.
