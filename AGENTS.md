# AGENTS.md

Guidance for AI agents working in this repository.

## Overview

**point** (聚·point) — a BBS/forum system with the “黑墨暖纸” reading-room aesthetic. Vermilion `#c43d3d` is the only accent color.

| Directory | Stack | Purpose |
|---|---|---|
| `point-frontend/` | Vue 3 + Vuetify + Vite + TypeScript | SPA |
| `point-*/` (7 Maven modules) | Java 25 + freeway 2.0.1-SNAPSHOT + Maven | REST backend |

Module chain: `point-domain` → `point-db` → `point-cache` → `point-service` → `point-web` + `point-admin` → `point-boot` (launcher).

Runtime shapes: `PointApp` (single machine, default) and `PointCloudApp` (cloud, explicit).

## Build & run

```bash
mvn -o clean test                        # 34 tests: service 28 + boot 6
mvn -o clean package -pl point-boot -am  # fat JAR, mainClass PointApp
java -jar point-boot/target/point-boot-*.jar --F2.profile=dev   # :8082, H2 at ./data/bbs

./point.sh build [--frontend] [--fast]   # build
./point.sh start|stop|restart|status|logs
./point.sh cloud [--profile dev|prod]    # cloud is always explicit
```

`point.sh` defaults to the `dev` profile on :8082; `restart` defaults to single-machine and asks before switching to cloud. Operational details live in `DEPLOYMENT.md`.

Frontend:

```bash
cd point-frontend
npm run dev      # Vite :3000, /api proxied to :8082
npm run build    # vue-tsc --noEmit + production build
npm run lint     # ESLint (flat config)
```

`bash tools/sync-frontend.sh` builds the SPA and copies `dist/` into `point-boot/src/main/resources/static/`.

## Configuration

JSON, layered: `application.json` overridden by `application-dev.json` / `application-prod.json`. Framework keys use the uppercase `F2.*` alias, normalized to `freeway.*` before merging.

| Key | Meaning |
|---|---|
| `F2.http.server.port` / `host` | HTTP listener |
| `F2.db.url` / `username` / `password` | H2 or PostgreSQL |
| `bbs.jwt.secret` / `expire-days` / `cookie-domain` | session signing |
| `bbs.jwt.cookie-secure` | `Secure` on the session cookie; unset ⇒ false only under `dev` (prod, and a no-profile boot that may still be behind TLS, get true) |
| `bbs.upload.dir` / `max-size` | uploads; default 10 MB |
| `bbs.admin.username` / `password` | bootstrap admin (password ≥ 8) |
| `F2.cloud.discovery.static.services` | cloud VIP: `serviceId=scheme://host:port[/basePath]` |

Secrets belong in a config file, not `-D`: freeway loads the file named by
`freeway.config.file` (env `FREEWAY_CONFIG_FILE`) at the top of its file tier.
`point.sh` writes the env overrides into `logs/point.runtime.json` (0600) for
exactly this reason — `ps` exposes argv to every local user. `bbs.*` keys are
outside the `FREEWAY_`/`F2_` env mapping, so they have no environment-variable form.

Demo data (password `123456`) seeds **only when the `dev` profile is explicitly
active**; no profile means an empty site plus roles/permissions. `prod` fails
fast on a placeholder JWT secret; anything public must use `prod`.

## Backend architecture

- **freeway** — `ModuleEx` + `Binder` composition; no SPI scanning (`autoDiscovery(false)`). `Module` wires routes, filters and seeds.
- **RuntimeHook** — ordered startup: schema → seeds → HTTP.
- **Container** — constructor injection; routes/filters/subscribers are contributions, not scanned.
- **DB** — `Schema.ensure` is idempotent; `db.query()` / `db.execute()`; dialect from the JDBC URL.
- **Auth** — HttpOnly `point_token` cookie, SameSite=Lax (+ `Secure` when configured); `Authorization` header also accepted, `?token=` deliberately is not. `AuthFilter` binds `CurrentUser` via `ScopedValue`. `AuthApi` is the in-process seam; cloud peers use `RpcExport` at `/rpc/auth/validateToken`.
- **Cloud** — `CloudModule` + config-driven `StaticModule`; no registry, heartbeat or discovery write side. `CloudHealthModule` contributes the database to `/health/ready` (`/health/live` and `/health/ready` are freeway's probes; `/healthz` stays the single-machine check).
- **Layering** — web/admin modules hold no SQL: reads and writes live in `point-service` services over `point-db` repositories. `ResponseEnricher` only shapes DTOs.
- **Request bodies** — annotate a record in `point-domain/.../dto` and parse it through `WebUtils.validatedBody` (web) or `AdminBodies.validated` (admin). Both turn decoder rejections and bean failures into 400 `validation_failed`; never `bodyAsJson(Map.class)`.
- **Startup** — `PointLauncher` owns logging/banner/port for both mains; the version comes from filtered `build-info.properties`, not a literal. Keep both `main` classes: `point.sh` reads the deployment shape from the process command line.

## Frontend

- Pinia auth store; session cookie, no token in localStorage.
- Axios `/api`; success `{code,message,data}`; thrown errors are `{error,code}` and the interceptor exposes `error` as `message`.
- One `DefaultLayout`; content layout `[main max 680 | PageAside 366]`, aside hidden below 1100 px.
- Paper themes via CSS variables + `useTheme()` + `localStorage.paperTheme`.
- Comment threads: `src/utils/thread.ts` orders depth-first and draws one conversation rail; used by topic, moment and article detail views.
- Image grid: `src/utils/markdown.ts` splits markdown text and consecutive images into `.img-grid`; do not replace it with a simpler renderer.

## Testing & CI

- Unit tests live beside code; `mvn -o clean test` = 24.
- API smoke: `bash tools/smoke-api.sh http://127.0.0.1:8082` (17 checks).
- E2E: `cd point-frontend && npm run test:e2e` (Playwright; first run `npx playwright install chromium`). The config starts `point.sh run --profile dev` unless `E2E_BASE_URL` points at a running instance; build the JAR first.
- CI: JDK 25 + Node 22; backend tests, frontend build/lint, smoke.
- For backend changes: clean test → clean package → boot the JAR and exercise the changed routes.

## Conventions & pitfalls

- Let the JDBC URL choose the dialect; do not force `freeway.db.dialect=h2`. H2 runs `MODE=PostgreSQL` with `NON_KEYWORDS=...`.
- `LIKE ... ESCAPE '!'`; `ESCAPE '\'` breaks under the MySQL lexer.
- JWT placeholder fails fast in `prod`; sessions are HttpOnly cookies — never reintroduce localStorage tokens.
- Security headers must run before `AuthFilter`, including on 401/403 responses.
- The SPA shell is the root mount's `index.html`, served for `/` by the mount's directory rule; there is no SPA fallback and no shell filter. The router is hash-based, so `/` is the only server-visible entry. A fallback (or a filter, which runs before route dispatch) answers unknown `/api/*`, `/health/*` and the `/ws` upgrade with HTML; those paths must stay JSON 404s.
- Errors go through `ErrorMapper`; the server writes the response. `PointErrorMapper` maps app exceptions. Do not add a writing error filter — 2.0 maps exceptions inside each filter level.
- Event subscribers that resolve `EventBus`-dependent services must be registered from `RuntimeHook.start`, not composition-time contributions.
- Deferred contributions (filters, subscribers) are constructed before extension stores seal. Do not inject the service graph into a filter; `AuthFilter` holds `Container` and resolves per request. Follow that pattern.
- `bbs_article` has no `last_comment_time`; only topics do.
- Static files: hashed assets immutable; `index.html` revalidates with `max-age=0` + ETag.
- After a freeway version bump: clean build and record the change below — the banner reads `freeway.version` from the filtered `build-info.properties`, so nothing to hand-edit.

## Version history

- **1.0.7** (2026-11-18) — freeway 2.0.1-SNAPSHOT; `F2` config alias; config-driven `StaticModule`; `PointErrorMapper`; `AuthFilter` lifecycle fix; `DEPLOYMENT.md` + `point.sh`; comment-thread rails.
- **1.0.6** — password change endpoint and profile UI.
- **2026-11-02** — data-logic audit: notification pipeline, soft-delete leaks, comment/article counters, quote references.

Older freeway 1.x migration history is available in git history.
