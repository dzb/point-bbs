<div align="center">
  <h1><img src="data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA2NCA2NCI+CiAgPHJlY3Qgd2lkdGg9IjY0IiBoZWlnaHQ9IjY0IiByeD0iMTIiIGZpbGw9IiNjNDNkM2QiLz4KICA8dGV4dCB4PSIzMiIgeT0iNDciIHRleHQtYW5jaG9yPSJtaWRkbGUiIGZvbnQtZmFtaWx5PSInTm90byBTZXJpZiBTQycsJ1NvdXJjZSBIYW4gU2VyaWYgU0MnLEdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNDQiIGZvbnQtd2VpZ2h0PSI3MDAiIGZpbGw9IiNmZGZhZjYiPuiBmjwvdGV4dD4KPC9zdmc+Cg==" alt="聚" width="48" height="48" style="vertical-align: middle; margin-right: 8px; border-radius: 8px;"> point</h1>
  <p><em>聚 · 记录思考，分享见闻</em></p>
  <p>A modern BBS/forum system with a quiet, reading-room aesthetic.</p>

  <!-- badges -->
  <p>
    <img src="https://img.shields.io/badge/Java-25-%23ED8B00?logo=openjdk&logoColor=white" alt="Java 25"/>
    <img src="https://img.shields.io/badge/Freeway-1.3.2-%238B5CF6?logo=data:image/svg%2Bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxNiIgaGVpZ2h0PSIxNiIgdmlld0JveD0iMCAwIDE2IDE2Ij48cGF0aCBkPSJNOCAwbDggNC04IDQtOC00eiIgZmlsbD0iI2ZmZiIvPjxwYXRoIGQ9Ik0wIDRsOCA0LTggNHoiIGZpbGw9IiNjNDNkM2QiLz48L3N2Zz4=&amp;label=Freeway&amp;labelColor=%23374151" alt="Freeway 1.3.2"/>
    <img src="https://img.shields.io/badge/Vue_3-3.5-%234FC08D?logo=vue.js&logoColor=white" alt="Vue 3"/>
    <img src="https://img.shields.io/badge/JDK_25_+_Vue_3_+_H2-SPA_%2B_REST-%23c43d3d" alt="SPA+REST"/>
  </p>
  <p>
    <img src="https://img.shields.io/badge/license-MIT-%23f0ad4e" alt="MIT License"/>
    <img src="https://img.shields.io/badge/build-Maven_shade_JAR-%234299e1?logo=apachemaven&logoColor=white" alt="Maven"/>
    <img src="https://img.shields.io/badge/status-active_development-%232ea043" alt="Status"/>
  </p>

  <p>──</p>
  <p>Companion application for the <a href="https://github.com/dzb/freeway">Freeway</a> Java framework — built to <strong>validate, exercise, and showcase</strong> Freeway's capabilities in a real-world project.</p>

  <p>
    <img src="https://img.shields.io/badge/%F0%9F%94%A8_Module_Composition-binder.install()_%2B_autoDiscovery(false)-%238B5CF6" alt="Module Composition"/>
    <img src="https://img.shields.io/badge/%F0%9F%93%9C_Schema_Migration-SchemaEntity_contribution-%234299e1" alt="SchemaEntity"/>
    <img src="https://img.shields.io/badge/%E2%9D%A4%EF%B8%8F_Injection-constructor_DI_%2B_%40Value-%23c43d3d" alt="DI"/>
    <img src="https://img.shields.io/badge/%F0%9F%93%A1_Events-EventSubscriber_%2B_EventBus-%232ea043" alt="Events"/>
    <img src="https://img.shields.io/badge/%F0%9F%93%8A_Health_Check-pluggable_HealthCheck-%23f0ad4e" alt="HealthCheck"/>
  </p>
</div>

---

## Design Philosophy

**黑墨暖纸 (Black Ink · Warm Paper)**

point is built around the idea of a digital reading room — calm, distraction-free, and text-first. The visual language draws from traditional Chinese calligraphy aesthetics: ink-black characters on warm paper, with vermilion (`#c43d3d`) as the sole accent color, used sparingly like a signature seal.

> 书斋美学 — 松烟墨色为字，朱砂一点为印，宣纸三色可择，夜读墨池相伴。

Four paper themes are available (cicada grey, warm ivory, golden amber, and night ink), all sharing the same vermilion accent.

## Tech Stack

| Layer | Technology |
|---|---|
| **Frontend** | Vue 3 + Vuetify 3 + Vite + TypeScript |
| **Backend** | Java (JDK 25) + Freeway framework |
| **Database** | H2 (file-based, auto-created at startup) |
| **Auth** | JWT (HMAC-SHA256) |
| **Build** | Maven (shade JAR) + npm |

### Freeway Framework

The backend is built on [Freeway](https://github.com/dzb/freeway), a modern Java framework with:

- Compose-first IoC container (`ModuleEx` / `Binder`)
- Embedded HTTP server with routing, filters, and static files
- Lightweight ORM with schema auto-migration
- Event bus for decoupled domain events
- Config cascade via `@Symbol` / `@Value` annotations

---

## Quick Start

**The whole backend ships as a single executable JAR** — zero external dependencies. No database (embedded H2), no web server (embedded), no node runtime needed for deployment.

### One-Command Deploy

```bash
# Requires JDK 25
mvn clean package -pl point-boot -am

# Everything in one JAR — starts on http://localhost:8082
java -jar point-boot/target/point-boot-*.jar
```

That's it. The JAR contains:
- The REST API server (Freeway embedded HTTP engine)
- The compiled frontend SPA (served as static resources)
- The **H2 file-based database** engine (auto-creates `./data/bbs` on first run — data persists across restarts)
- Schema migration and seed data (auto-run at startup)

No Docker, no PostgreSQL, no nginx, no Node.js.

### Frontend Dev Mode (optional)

If you're working on the frontend separately:

```bash
cd point-frontend
npm run dev            # http://localhost:3000, API proxied to :8082
npm run build          # Production build → point-boot serves it
```

---

## Project Structure

```
point-bbs/
├── point-boot/           # Executable launcher (shade JAR)
├── point-domain/         # Entities, DTOs, events
├── point-db/             # Repositories (DB access layer)
├── point-cache/          # Simple in-memory cache
├── point-service/        # Business logic services
├── point-web/            # Public REST API routes
├── point-admin/          # Admin panel REST API routes
├── point-frontend/       # Vue 3 SPA
│   └── src/
│       ├── api/          # HTTP client + API calls
│       ├── components/   # Shared UI components
│       ├── composables/  # Vue composables (theme, etc.)
│       ├── layouts/      # App shell (DefaultLayout)
│       ├── router/       # Vue Router config
│       ├── stores/       # Pinia stores (auth)
│       ├── types/        # TypeScript interfaces
│       ├── utils/        # markdown rendering, etc.
│       └── views/        # Page components
│           ├── article/  # Article pages
│           ├── auth/     # Login / Register
│           ├── home/     # Home / Explore / Following / Search
│           ├── topic/    # Topic pages
│           └── user/     # Profile / Messages / Favorites
├── TODO.md               # Remaining unconnected features
├── DESIGN.md             # Full design system documentation
└── CLAUDE.md             # AI workspace instructions
```

---

## Architecture

### Backend Module Chain

```
point-domain → point-db → point-cache → point-service → point-web (+ point-admin) → point-boot
```

Each module builds on the one before it. `point-boot` is the entry point that wires everything together.

### Page Layout Convention

```
│ sidebar 240px │  main content (flex 1, max 680px)  │  aside 366px │
```

- Three-column layout on desktop
- Sidebar collapses to icons at <1200px
- Aside hides at <1100px
- Responsive breakpoints: 1300/1200/1100/900px

### Data Flow

```
Vue SPA ──HTTP/JSON──> REST API (point-web) ──> Service Layer ──> Repository ──> Database
```

API responses follow `{ code: number, message: string, data: T }`. Paginated list endpoints return `{ items: [], page, pageSize, total }`.

---

## Features

### Core
- **Topics** — Create, edit, delete text posts with markdown support
- **Moments** — Lightweight image-rich posts (like tweets) for the home feed
- **Articles** — Long-form writing with markdown toolbar
- **Comments** — Nested replies with @mentions and inline reply
- **Search** — Full-text title search on topics

### Social
- **Follow** users to see their moments in your following feed
- **Like** — Like any topic, article, or comment
- **Favorite** — Bookmark topics and articles
- **Notifications** — Get notified on comments, likes, follows, and @mentions

### Reading Experience
- **Four paper themes** — cicada grey, warm ivory, golden amber, night ink
- **Vermilion accent** — `#c43d3d` is the only accent color, used sparingly
- **Image grid** — 2+ consecutive images auto-form into a responsive grid
- **Responsive layout** — Optimized from desktop to mobile

---

## API Overview

The REST API is served under `/api` on port 8082. Key endpoints:

### Auth
| Method | Path | Description |
|--------|------|-------------|
| POST   | `/api/auth/signin` | Sign in (email or username) |
| POST   | `/api/auth/signup` | Create account |
| GET    | `/api/auth/github/authorize` | GitHub OAuth authorization URL |

### Topics
| Method | Path | Description |
|--------|------|-------------|
| GET    | `/api/topics` | Recent topics (paginated) |
| GET    | `/api/topics/moments` | Moments feed |
| GET    | `/api/topics/following` | Following feed (auth required) |
| GET    | `/api/topics/search?q=` | Search topics by title |
| POST   | `/api/topics` | Create topic |
| GET    | `/api/topics/{id}` | Topic detail |
| POST   | `/api/topics/{id}/like` | Like topic |
| POST   | `/api/topics/{id}/favorite` | Favorite topic |

### Articles
| Method | Path | Description |
|--------|------|-------------|
| GET    | `/api/articles` | Recent articles (paginated) |
| POST   | `/api/articles` | Create article |
| GET    | `/api/articles/{id}` | Article detail |

### Users
| Method | Path | Description |
|--------|------|-------------|
| GET    | `/api/users/current` | Current user info |
| GET    | `/api/users/search?q=` | User search for @mention |
| GET    | `/api/users/{id}` | User profile |
| GET    | `/api/users/{id}/topics` | User's topics |
| GET    | `/api/users/{id}/messages` | User's notifications |

### Comments
| Method | Path | Description |
|--------|------|-------------|
| GET    | `/api/topics/{id}/comments` | Topic comments |
| POST   | `/api/topics/{id}/comments` | Add comment to topic |
| GET    | `/api/articles/{id}/comments` | Article comments |
| POST   | `/api/articles/{id}/comments` | Add comment to article |

---

## Configuration

Configuration lives in `point-boot/src/main/resources/application.json` (overridden per profile by `application-dev.json` / `application-prod.json`):

```json
{
  "web.server.port": 8082,
  "freeway.db.url": "jdbc:h2:file:./data/bbs",
  "bbs.jwt.secret": "change-me-in-production",
  "bbs.upload.dir": "./uploads",
  "bbs.oauth.github.client-id": "",
  "bbs.oauth.github.client-secret": ""
}
```

---

## License

MIT
