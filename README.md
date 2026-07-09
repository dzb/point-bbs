<div align="center">
  <h1>📖 point</h1>
  <p><em>聚 · 记录思考，分享见闻</em></p>
  <p>A modern BBS/forum system with a quiet, reading-room aesthetic.</p>
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

The backend is built on [freeway](https://github.com/dzb/freeway), a modern Java framework with:

- Compose-first IoC container (`ModuleEx` / `Binder`)
- Embedded HTTP server with routing, filters, and static files
- Lightweight ORM with schema auto-migration
- Event bus for decoupled domain events
- Config cascade via `@Symbol` / `@Value` annotations

---

## Quick Start

### Backend

```bash
# Build the fat JAR (requires JDK 25)
mvn clean package -pl point-boot -am

# Run (starts on http://localhost:8082)
java -jar point-boot/target/point-boot-*.jar
```

The server starts with an H2 file database at `./data/bbs` — no external database setup needed. Schema and seed data are auto-migrated on first run.

### Frontend

```bash
cd point-frontend

# Dev server (http://localhost:3000, API proxied to :8082)
npm run dev

# Production build
npm run build

# Preview production build
npm run preview
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
