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

## Project Structure

```
apps/
  api/          # Spring Boot 3.3 backend (Java 21)
  web/          # React 18 frontend (phases 14+)
docs/
  ARCHITECTURE.md
  IMPLEMENTATION_PLAN.md
infra/          # Docker Compose (phase 18)
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
| 11–18 | Pending |

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

## License

Private — all rights reserved.
