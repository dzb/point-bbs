# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

**point** (聚·point) — a modern BBS/forum system. The name means "gather" in Chinese. The design philosophy is "黑墨暖纸" (Black Ink · Warm Paper): a quiet, reading-room aesthetic where vermilion (#c43d3d) is the sole accent color, used sparingly.

Two independent projects live in this repo:

| Directory | Stack | Purpose |
|---|---|---|
| `point-frontend/` | Vue 3 + Vuetify + Vite + TypeScript | SPA frontend |
| `point-boot/` + sibling modules | Java (JDK 25) + freeway 1.3.9 + Maven | REST API backend |

The backend source spans **7 Maven modules** at the repo root: `point-domain`, `point-db`, `point-cache`, `point-service`, `point-web`, `point-admin`, `point-boot`. The `point-boot` module is the executable launcher (shade JAR with `PointApp.main()`).

## Build & run

### Frontend

```bash
cd point-frontend
npm run dev        # Vite dev server on :3000, API proxied to :8082
npm run build      # Type-check (vue-tsc --noEmit) then production build
npm run preview    # Preview production build
```

`@/` maps to `src/` via Vite alias and tsconfig paths.

### Backend

```bash
mvn clean package -pl point-boot -am   # Build boot JAR + all dependencies
java -jar point-boot/target/point-boot-*.jar   # Run (starts on :8082)
```

The parent POM requires **JDK 25**. The boot JAR uses maven-shade-plugin to produce a fat JAR with `PointApp` as `mainClass`. H2 file-based database at `./data/bbs` (auto-created). Configuration is JSON-based at `point-boot/src/main/resources/application.json` (overridden per profile by `application-dev.json` / `application-prod.json`).

**Key config paths:**
- `web.server.port` — HTTP port (default 8082)
- `freeway.db.url` — H2 JDBC URL
- `bbs.jwt.secret` — JWT signing key
- `bbs.upload.dir` — file upload directory

The schema is auto-migrated at startup via `Schema.ensure(db, ALL_ENTITIES)` in `PointModule`. Default roles and permissions are seeded idempotently.

## Architecture

### Backend (freeway framework)

The backend is built on **freeway**, a custom lightweight Java framework (`C:\Users\Z13\Projects\freeway`). It provides its own IoC container, HTTP server (Robaho), DB layer, and module system. Key freeway concepts:

- **Module** — implements `com.jujin.freeway.ioc.Module`; discovered via ServiceLoader. `PointModule` is the root module that wires everything together.
- **RuntimeHook** — lifecycle hooks (`start`/`stop`) ordered via `.before()`/`.after()` chains. Used for schema migration → data seeding → AppContext init → HTTP server start.
- **Container** — the IoC container. Services, controllers, and other components are auto-discovered and injected.
- **Database** — freeway's DB abstraction. `Schema.ensure()` handles DDL from annotated entity classes. `db.query()` / `db.execute()` for SQL.
- **Web controllers** — discovered in `point-web/` and `point-admin/`. Routes are mapped by convention or annotation.

Module dependency chain: `point-domain` (entities) → `point-db` → `point-cache` → `point-service` → `point-web` + `point-admin` → `point-boot`.

The original bbs-go source is at `C:\Users\Z13\Projects\bbs-go` (for reference when working on the Java side).

### Frontend (Vue 3 SPA)

**State management:** Pinia (`src/stores/auth.ts`). The auth store manages JWT token in localStorage and current user state. Token is injected into all API requests via axios interceptor (`src/api/client.ts`).

**Routing:** Vue Router with a single layout (`DefaultLayout.vue`) wrapping all child routes. Every page uses lazy-loading (`() => import(...)`).

**HTTP client:** `src/api/client.ts` — a pre-configured axios instance with `/api` base URL, bearer token injection, and 401 → redirect-to-login handling. API responses follow `{ code: number, message: string, data: T }` shape.

**Component architecture:**
- `DefaultLayout.vue` — app shell: sidebar + top header (nav tabs, search, theme switcher, auth controls) + `<router-view />`
- `AppSidebar.vue` — sticky left sidebar (240px, collapses to 60px icons below 1200px)
- `PageAside.vue` — right-side community panel (366px, hidden below 1100px)
- `UserAvatar.vue` — consistent avatar rendering (image or initial-based colored fallback)
- `MomentCard.vue` — timeline card with image viewer, inline reply, and like toggling

**Page layout convention** — all content pages follow the same 2-column structure:
```
[main content: flex 1, max-width 680px] | [PageAside: 366px]
```
Main content gets `border-right` separator; aside is hidden below 1100px. Responsive breakpoints: 1300px, 1200px, 1100px, 900px.

### Theme system

Four paper themes implemented via CSS variables + Vuetify `useTheme()` + `<html>` class toggling:

| Key | Label | Dark? |
|---|---|---|
| `cicada` | 蝉翼·冷灰 (default) | No |
| `night` | 夜读·墨池 | Yes |

Theme is persisted to `localStorage` key `paperTheme`. Toggled via sun/moon icon button in the header (`usePaperTheme().toggleTheme()`). All component styles use `var(--paper-bg)`, `var(--paper-nav)`, `var(--paper-text)`, `var(--paper-text2)`, `var(--paper-border)`. Accent colors use `var(--paper-accent)` / `var(--paper-accent-hover)`.

### Design tokens

- Vermilion: `#c43d3d` (--paper-accent), `#a83434` (--paper-accent-hover), `#8e2626` (active)
- Fonts: headings use `'Noto Serif SC', 'Source Han Serif SC', Georgia, serif`; body uses system sans-serif
- Cards: no shadow (`elevation: 0`), 1px solid border, 8px border-radius
- Buttons: always lowercase, no letter-spacing, 6px border-radius
- Article body: 17px, line-height 1.9, max-width 680px centered

Full design system documented in `point-frontend/DESIGN.md`.

### Image grid (DO NOT SIMPLIFY)

`src/utils/markdown.ts` — `renderMarkdown()` splits content into text segments (rendered by markdown-it) and consecutive image groups (rendered as `<div class="img-grid cols-N">` directly). This is the ONLY approach that works with `breaks:true` + XSS-safe `html:false`. Simpler alternatives (pre-injecting HTML, post-processing rendered output) were tested and failed. The code comment documents all three approaches and why each simpler one was rejected.

## Recent changes (2026-08-15)

### Audit round: security + correctness + quality (2026-08-15)
- Verified freeway HEAD == 1.3.8-SNAPSHOT (b6ff921); reinstalled HEAD-level artifacts; all 7 modules compile unchanged; full API smoke + tests green. See `AUDIT-2026-08-15.md` for the complete report.
- **Search fix (dialect lexer conflict)**: `LIKE ... ESCAPE '\'` broke under the MySQL-dialect lexer (H2 MODE=MySQL URL derives MySqlDialect; `\'` = escaped quote swallows `$limit/$offset` → 500). H2 runtime wants `'\'`, real MySQL wants `'\\'` — dialect-neutral fix: `ESCAPE '!'` + `!`-escaping in TopicRepository.
- **Security**: PBKDF2 password hashing (legacy SHA-256 auto-upgrade on login); constant-time HMAC compare; JWT placeholder-secret fail-fast (prod) / loud warning (dev); OAuth redirect pinned to configured redirect-uri origin; upload restricted to `image/*` with sanitized filenames + nosniff + attachment disposition for non-images; followers/following/profile no longer expose password/email/phone; unique composite indexes on user_like/favorite/follow/third_user/username/email (H2: created via seed hook because MySQL-dialect introspection fails on H2; prod MySQL via Schema.ensure); auth rate limiting (10/10min per IP); signup/signin now use freeway typed-body validation; error handler no longer leaks exception class names.
- **Correctness**: comment delete now decrements parent + user counts (transactional); topic delete decrements topic_count; comment create increments user count; `getRecentTopics` total now counts type=0 only; deleted topics return 404; ArticleService partial updates + generated-key insert; unlike/unfollow/favorite transactional; event subscribers isolated (best-effort notifications); 400s instead of 500s for bad params/empty bodies; long offsets; pageSize clamps.
- **Bootstrap**: data-seed creates a config-driven admin (`bbs.admin.username`/`password`, no-op by default); dev seed grants admin role to 墨客 (id 1) idempotently; dev seed skipped on prod profile; seeded topic/article counters fixed up.
- **Frontend**: fixed critical stored XSS in markdown image-grid path (validateLink + space-encoding + escapeHtml); MomentCard comment envelope; upload promise hang; ArticleDetail error handling + route watch; read-all messages; auth store awaits full user; route guards + scrollBehavior; SearchPage query watch; favorites article navigation; mention debounce cleanup; optimistic-toggle error handling.
- **Production target switched from MySQL to real PostgreSQL** (`application-prod.json` now uses `jdbc:postgresql://localhost:5432/bbs`, no forced dialect — the URL derives `PostgresDialect`; `org.postgresql:postgresql` 42.7.7 added as a runtime driver in `point-boot`). All `@Column(type="LONGTEXT")` became dialect-neutral `TEXT`. Verified end-to-end against a real PostgreSQL 18.4 instance: Schema.ensure creates all 21 tables + 20 indexes (14 query + 6 unique) natively, seed/bootstrap/admin grant run, 16/16 API smoke, audit logs, JSON notifications and a 50k-char TEXT body all pass. Default config remains H2 `MODE=PostgreSQL` for zero-setup dev; the dev H2 and prod PG now share one dialect family.
- **H2 switched to PostgreSQL compatibility mode** (`MODE=PostgreSQL` in the JDBC URL, dev + tests): the URL now derives `PostgresDialect`, whose `INFORMATION_SCHEMA.INDEXES` introspection works natively on H2 — `Schema.ensure` now creates every entity-declared `@Index` (13 query + 6 unique) by itself, the MySQL-flavored introspection warnings are gone, and `CREATE INDEX IF NOT EXISTS` is native H2/Postgres syntax. Legacy MySQL-mode databases still open fine (identifiers were stored upper-cased; keep `NON_KEYWORDS` in the URL and avoid quoted identifiers — `bbs_sys_config.key` is referenced bare). The one index `@Index` cannot express (`idx_follow_other`, since `@Index` is not repeatable and `other_id` already belongs to unique `uq_user_follow`) is created by a one-line seed hook. Prod MySQL is unaffected.

### X-style comments (2026-08-15)
- Comment replies follow the Twitter/X flat-timeline model — **not** 楼中楼 (threaded nesting): all comments are flat rows ordered by time; a reply carries `quoteId` and is rendered with a quote-preview block of the replied-to content, an auto-`@nickname ` composer prefix, and a "回复" affordance on each comment. Posting appends the new comment and scrolls to it (visible even beyond page 1). The backend enriches comments with `quoteContent` (batch-queried, no N+1) and counts replies against the quoted comment + notifies its author.

### Optimization round 2 — full 15-item list (2026-08-15)
- OAuth state via HttpOnly SameSite=Lax cookie (login-CSRF closed); signout now clears the session cookie.
- Comment replies activated: quoteId > 0 bumps the quoted comment's count and notifies its author.
- WebSocket push: `/ws/notify` (token or cookie auth) + NotificationHub + event-driven pings; SPA refreshes unread on ping, 15s polling stays as fallback.
- Admin UI at `/admin` (topics/users/categories/config tabs) with `requiresAdmin` guard + `/api/users/current/permissions`; sidebar entry when admin.
- Tests: AuthService/UserService/CommentService suites (19 total, all green) + `tools/smoke-api.sh` (16 end-to-end checks). The new tests caught a real bug: hand-rolled JWT claim extraction broke on escaped quotes → payload parsing now uses freeway JsonUtils.
- CI: `.github/workflows/ci.yml` (JDK 25 + Node 22, Sonatype snapshots repo for freeway, backend tests + frontend build/lint + smoke).
- ESLint (flat config, typescript-eslint, eslint-plugin-vue) + Prettier + `.editorconfig`; `npm run lint` / `npm run format:check`.
- Frontend types: PageResult/AuthResult/ApiEnvelope; `any` refs removed from 8 views.
- List endpoints unified on `{items,page,pageSize,total}` (following/moments/recommended/articles).
- Dead code removed: 5 unpublished event classes, `User.roles` column, 12 unused methods.
- System.out/err → slf4j across services/modules.
- View-count writes throttled to one UPDATE per topic per 60s.
- **Session auth migrated to HttpOnly cookie**: signin/signup/OAuth set `point_token` (HttpOnly; SameSite=Lax = built-in CSRF protection since all mutations are POST); AuthFilter + WS endpoint read the cookie; frontend no longer stores the JWT in localStorage; `bbs.jwt.cookie-domain` config (dev sets `localhost` for the Vite proxy). Legacy Bearer header and `?token=` still accepted.
- Attachments: upload size cap (`bbs.upload.max-size`, default 10MB); downloads stream instead of buffering.

### Optimization round (2026-08-15, same day)
- SVG uploads rejected (script-capable image type); attachment disposition fallback.
- Global security headers filter (`SecurityHeadersFilter`): CSP self-only + X-Frame-Options DENY + nosniff + Referrer-Policy; ordered before SpaFilter so SPA routes carry them too.
- Permission codes cached 60s per user (PermissionService + SimpleCache); invalidated on role changes — AuthFilter no longer runs a 3-table JOIN per request.
- Frontend bundle -82%: `vite-plugin-vuetify` auto-import replaces the full Vuetify import; vendor manualChunks (vue/markdown-it). index chunk 722KB → 110KB (gzip 234KB → 41KB).
- Dependencies aligned with freeway HEAD: slf4j 2.0.18, H2 2.4.240 (existing 2.3 data file opens fine).
- `tools/sync-frontend.sh` — builds the frontend and copies dist into `point-boot/src/main/resources/static/` (previously a manual step).

## Recent changes (2026-06-14)

### freeway 1.3.8-SNAPSHOT upgrade (2026-08-14)
- `freeway.version` 1.3.2 → 1.3.8-SNAPSHOT in parent POM
- `Row.longVal()` → `Row.longValue()` rename (13 files)
- `PostgresDialect` moved to `com.jujin.freeway.db.dialect`; `Schema.ensure()` no longer takes a dialect (derived from URL) — TopicServiceTest updated
- HTTP API renames: `headerSet()` → `setHeader()`, `status(int)` → `setStatus(int)`, `ExceptionMapper` → `ErrorHandler` (handler lambda now receives `HttpResponse`; request method/path no longer available in the handler)
- Config keys: `freeway.web.*` → `freeway.http.*` (server/cors) in all three profile JSONs; `freeway.db.*` unchanged; prod config pins `"dialect": "mysql"`
- `PointApp` now forwards CLI args via `.args(args)` and reads `freeway.http.server.port` for the startup banner
- Note: do NOT force `"dialect": "h2"` — H2Dialect inherits Postgres dollar-quoting, which breaks `$named` SQL params (silent empty results)


### Layout restructure
- Full-width top header with logo + tabs + search + user menu
- Three-column body: sidebar nav (240→60px), main content, aside (366→0px)
- Logo moved from sidebar into header, left-aligned to sidebar icons (12px padding)
- Tabs left-aligned to content area start position
- Responsive breakpoints centralized in `DefaultLayout.vue` (1300/1200/1100/900)
- Aside extracted from pages into Vue Router named views (`<router-view name="aside">`)
- ~150 lines of duplicated CSS removed; shared styles consolidated to global block

### Pagination (30 items/page + load-more)
- Unified pageSize to 30 across all list/comment endpoints
- Added "显示更多" to 11 list types (HomePage, FollowingPage, ExplorePage, ArticleList, SearchPage, MessagesPage, FavoritesPage, UserProfile topics+articles, ArticleDetail comments, TopicDetail comments, MomentCard comments)

### Backend audit fixes (16 issues across 5 rounds)
**Critical:** AdminCategoryRoutes empty CRUD → implemented; production JWT secret empty → placeholder; count() full-table scan → SELECT COUNT(*); AdminFilter RBAC bypass → seeded admin permission code
**Security:** SQL injection in IN clauses (3 locations) → parameterized; ownership checks missing (4 endpoints) → added; LIKE wildcard escaping → ESCAPE clause
**Design:** ResponseEnricher missing fields → added imageList/hideContent/tags; parseInt duplicated 4× → extracted then replaced with HttpContext coercion; ArticleService DI inconsistency → constructor injection; signup missing avatar → aligned with signin
**Quality:** SimpleCache race/leak → computeIfAbsent+maxSize+cleanExpired; ForbiddenWord visibility → private; cache generics Object→typed; @Column annotations added to 6 entities; ArticleTag status long→int; GitHub OAuth URL → configurable; default nickname → hex timestamp; admin user list → paginated

### Frontend audit fixes (14 issues across 5 rounds)
**Critical:** XSS via `html:true` → removed, image grids post-processed; prop mutation → local ref; memory leak → onUnmounted cleanup
**Types:** Created `src/types/` with Topic/Article/Comment/Message/UserInfo interfaces; replaced `any` refs
**Error handling:** 23 empty catch blocks → console.error; TopicEdit added validation + try/catch
**Consistency:** MarkdownIt singleton; empty style blocks removed; img-grid.css fixed; viewer dark mode; 401/logout use router.push instead of hard navigation; aria-labels on key buttons
**Cleanup:** 2 pre-existing TS errors fixed (v-badge type, Promise generic)

### API alignment
- 35 frontend API calls all match backend routes exactly, zero mismatches
- 31 backend routes unused by frontend (OAuth, article edit/delete, admin panel, etc.)
- See full inventory in audit notes below

## Backend route inventory (2026-06-14 state)

### Used by frontend (35 calls matching):
- Auth: signup, signin
- Topics: list, moments, following, search, detail, create, edit, delete, comments CRUD, like/unlike, favorite/unfavorite, status checks
- Articles: list, detail, create, comments CRUD
- Users: current, profile, topics, articles, messages CRUD, follow/unfollow, favorites
- Upload: POST /upload
- Categories: GET /categories

### Not yet wired (31 routes):
- OAuth: GitHub authorize/callback/bind/unbind (5)
- Topic recommended (1)
- Article edit/delete, like/unlike/favorite/unfavorite (6)
- User edit, followers/following list (3)
- Attachment download (1)
- Admin panel: topic(6) + user(5) + category(4) + config(4) = 19
