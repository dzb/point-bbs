# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

**point** (聚·point) — a modern BBS/forum system. The name means "gather" in Chinese. The design philosophy is "黑墨暖纸" (Black Ink · Warm Paper): a quiet, reading-room aesthetic where vermilion (#c43d3d) is the sole accent color, used sparingly.

Two independent projects live in this repo:

| Directory | Stack | Purpose |
|---|---|---|
| `point-frontend/` | Vue 3 + Vuetify + Vite + TypeScript | SPA frontend |
| `point-boot/` + sibling modules | Java (JDK 25) + freeway 1.1.0 + Maven | REST API backend |

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

## Recent changes (2026-06-14)

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
