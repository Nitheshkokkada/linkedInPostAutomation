# LinkedIn AI Agent

Multi-agent SaaS that researches topics, creates LinkedIn posts + images, schedules, publishes, tracks analytics, and self-improves — designed entirely for free-tier services.

## Prerequisites

- **Java 21** (JDK, not JRE 8)
- **Maven 3.9+**
- **Node.js 20+** (for frontend, phases 14+)
- **Docker** (optional, for local PostgreSQL)
- Accounts: [Supabase](https://supabase.com), [Google AI Studio](https://aistudio.google.com), [LinkedIn Developer](https://developer.linkedin.com), [Tavily](https://tavily.com)

## Quick Start (Backend)

1. Copy environment template:
   ```bash
   cp .env.example .env
   ```

2. Fill in `.env` values (Supabase DB URL, API keys, etc.)

3. Run the API:
   ```bash
   cd apps/api
   mvn spring-boot:run
   ```

4. Verify health:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

5. Register and login:
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/register \
     -H "Content-Type: application/json" \
     -d '{"email":"you@example.com","password":"password123","fullName":"Your Name"}'
   ```

## Quick Start (Frontend)

1. Install dependencies:
   ```bash
   cd apps/web
   npm install
   ```

2. Start dev server:
   ```bash
   npm run dev
   ```

3. Open http://localhost:5173 in your browser.

## Docker

1. Copy environment template:
   ```bash
   cp .env.example .env
   ```

2. Fill in `.env` values

3. Start all services:
   ```bash
   cd infra
   docker-compose up -d
   ```

4. Access the app at http://localhost:8080 (API) or http://localhost:5173 (frontend dev)

## Project Structure

```
apps/
  api/          # Spring Boot 3.3 backend (Java 21)
  web/          # React 18 frontend (Vite + TypeScript + Tailwind)
docs/
  ARCHITECTURE.md
  IMPLEMENTATION_PLAN.md
infra/          # Docker Compose + Nginx
scripts/        # Seed SQL
```

## Implementation Progress

See [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md) for phased build status.

| Phase | Status |
|-------|--------|
| 1 — Database schema + entities | Done |
| 2 — JWT authentication | Done |
| 3 — LinkedIn OAuth | Done |
| 4 — Gemini rate limiting | Done |
| 5 — TopicResearchAgent | Done |
| 6 — ContentCreationAgent | Done |
| 7 — ReviewAgent | Done |
| 8 — ImageAgent | Done |
| 9 — SchedulerAgent + Quartz | Done |
| 10 — LinkedInPublishingAgent | Done |
| 11 — AnalyticsAgent | Done |
| 12 — SelfLearningAgent | Done |
| 13 — Remaining REST endpoints | Done |
| 14 — React frontend: auth + dashboard + GeminiUsageBar | Done |
| 15 — Posts + schedule + analytics pages | Done |
| 16 — Agent monitoring page | Done |
| 17 — Tests + CI pipeline | Done |
| 18 — Docker Compose + deploy config | Done |

## Running Tests

```bash
cd apps/api
mvn test
```

Requires Java 21 and Docker (Testcontainers for integration tests).

## API Endpoints (implemented)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/auth/register` | Create account |
| POST | `/api/v1/auth/login` | Login (access token + refresh cookie) |
| POST | `/api/v1/auth/refresh` | Rotate tokens |
| POST | `/api/v1/auth/logout` | Clear refresh cookie |
| GET | `/api/v1/auth/linkedin/connect` | Start LinkedIn OAuth (auth required) |
| GET | `/api/v1/auth/linkedin/callback` | OAuth callback |
| DELETE | `/api/v1/auth/linkedin/disconnect` | Remove LinkedIn connection |
| GET | `/api/v1/me` | Current user profile |
| GET | `/api/v1/usage/gemini` | Gemini daily usage stats |
| GET | `/api/v1/dashboard` | Dashboard summary |
| GET | `/api/v1/topics` | List all topics |
| GET | `/api/v1/topics/active` | List active topics |
| GET | `/api/v1/topics/{id}` | Get topic by ID |
| POST | `/api/v1/topics` | Create topic |
| PUT | `/api/v1/topics/{id}` | Update topic |
| DELETE | `/api/v1/topics/{id}` | Delete topic |
| PATCH | `/api/v1/topics/{id}/toggle-active` | Toggle topic active status |
| GET | `/api/v1/posts` | List posts (paginated) |
| GET | `/api/v1/posts/status/{status}` | List posts by status |
| GET | `/api/v1/posts/{id}` | Get post by ID |
| PATCH | `/api/v1/posts/{id}` | Update post |
| POST | `/api/v1/posts/{id}/approve` | Approve post |
| POST | `/api/v1/posts/{id}/reject` | Reject post |
| DELETE | `/api/v1/posts/{id}` | Delete post |
| GET | `/api/v1/schedule` | List scheduled posts |
| GET | `/api/v1/schedule/day` | Get posts for specific day |
| GET | `/api/v1/schedule/{id}` | Get scheduled post by ID |
| GET | `/api/v1/schedule/queued-count` | Get queued posts count |
| DELETE | `/api/v1/schedule/{id}` | Cancel scheduled post |
| GET | `/api/v1/analytics` | List all analytics |
| GET | `/api/v1/analytics/post/{id}` | Get analytics for post |
| GET | `/api/v1/analytics/engagement-rate` | Average engagement rate |
| GET | `/api/v1/analytics/published-posts` | List published posts |
| GET | `/api/v1/analytics/published-count` | Get published count |
| GET | `/api/v1/settings` | Get user settings |
| PATCH | `/api/v1/settings` | Update user settings |
| GET | `/api/v1/agents/logs` | List agent logs (paginated) |
| GET | `/api/v1/agents/logs/agent/{name}` | Filter logs by agent name |
| GET | `/api/v1/agents/logs/status/{status}` | Filter logs by status |

## License

Private — all rights reserved.
