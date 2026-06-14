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
| `jade` | 象牙·玉白 (default) | No |
| `jinsu` | 金粟·暖黄 | No |
| `cicada` | 蝉翼·冷灰 | No |
| `night` | 夜读·墨池 | Yes |

Theme is persisted to `localStorage` key `paperTheme`. The `usePaperTheme()` composable (`src/composables/usePaperTheme.ts`) orchestrates applying CSS variables and Vuetify theme colors. All component styles use `var(--paper-bg)`, `var(--paper-text)`, `var(--paper-text2)`, `var(--paper-border)` instead of hardcoded colors. The vermilion (`#c43d3d`) accent is the only non-variable color used throughout.

### Design tokens

- Vermilion: `#c43d3d` (default), `#a83434` (hover), `#8e2626` (active)
- Fonts: headings use `'Noto Serif SC', 'Source Han Serif SC', Georgia, serif`; body uses system sans-serif
- Cards: no shadow (`elevation: 0`), 1px solid border, 8px border-radius
- Buttons: always lowercase, no letter-spacing, 6px border-radius
- Article body: 17px, line-height 1.9, max-width 680px centered

Full design system documented in `point-frontend/DESIGN.md`.
