# LinkedIn AI Agent — Implementation Plan

> Phased build order. Each phase must be fully complete (including tests) before the next begins.
> Deferred items use `TODO(phase-2):` in code — do not implement carousel, video, multi-language, etc. in v1.

---

## Progress Tracker

| Phase | Description | Status |
|-------|-------------|--------|
| 1 | Flyway schema + JPA entities + repositories | **Done** |
| 2 | Spring Boot skeleton + JWT auth endpoints | **Done** |
| 3 | LinkedIn OAuth + AES token encryption | **Done** |
| 4 | GeminiRateLimiter + gemini_usage tracking | **Done** |
| 5 | TopicResearchAgent + Tavily integration | **Done** |
| 6 | ContentCreationAgent | **Done** |
| 7 | ReviewAgent + Flesch-Kincaid util | **Done** |
| 8 | ImageAgent (Gemini image gen + Supabase upload) | **Done** |
| 9 | SchedulerAgent + Quartz wiring | **Done** |
| 10 | LinkedInPublishingAgent | **Done** |
| 11 | AnalyticsAgent | **Done** |
| 12 | SelfLearningAgent | **Done** |
| 13 | Remaining REST endpoints | **Done** |
| 14 | React frontend: auth + dashboard + GeminiUsageBar | **Done** |
| 15 | Posts + schedule + analytics pages | **Done** |
| 16 | Agent monitoring page | **Done** |
| 17 | Tests + CI pipeline | **Done** |
| 18 | Docker Compose + deploy config | **Done** |

---

## Phase 1 — Database Foundation ✅

**Deliverables**
- `V1__init.sql` — 11 application tables
- `V2__quartz.sql` — Quartz JDBC tables
- 11 JPA `@Entity` classes with `@UuidGenerator`, `jsonb` via `@JdbcTypeCode`
- 11 `JpaRepository` interfaces
- `application.yml` with datasource, Flyway, Quartz, Resilience4j config
- `pom.xml` with all dependencies

**Verification**
- App starts against Supabase/PostgreSQL
- Flyway migrations apply cleanly
- Hibernate `ddl-auto: validate` passes

---

## Phase 2 — JWT Authentication (current)

**Deliverables**
- `AppProperties` — typed config binding
- `SecurityConfig` — public `/api/v1/auth/**`, JWT filter on protected routes
- `JwtTokenProvider` — access (15 min) + refresh (7 d) token generation/validation
- `JwtAuthenticationFilter` — Bearer token extraction
- `AuthController` — register, login, refresh, logout
- `AuthService` — BCrypt hashing, user creation, token issuance
- `GlobalExceptionHandler` — structured `{"error","code"}` responses
- `ErrorResponse` record + domain exceptions
- Unit + integration tests (WireMock not needed yet; `@WebMvcTest` + Testcontainers)

**Endpoints**
| Method | Path | Auth |
|--------|------|------|
| POST | `/api/v1/auth/register` | Public |
| POST | `/api/v1/auth/login` | Public |
| POST | `/api/v1/auth/refresh` | Public (refresh cookie) |
| POST | `/api/v1/auth/logout` | Public |

**Verification**
- `POST /auth/register` creates user, returns access token
- `POST /auth/login` returns access token + sets HttpOnly refresh cookie
- `POST /auth/refresh` rotates tokens
- Protected endpoint returns 401 without token, 200 with valid token

---

## Phase 3 — LinkedIn OAuth

- `EncryptionUtil` — AES-256 for `linkedin_access_token`
- `LinkedInOAuthHandler` — authorization code flow
- Endpoints: connect, callback, disconnect
- Store encrypted token + profile metadata in `users`

---

## Phase 4 — Gemini Rate Limiting

- `GeminiRateLimiter` — Resilience4j `@RateLimiter` wrapper
- `GeminiUsageService` — daily counter in `gemini_usage` table
- `BudgetExceededException` → HTTP 429 `GEMINI_DAILY_LIMIT_REACHED`
- `SpringAIConfig` — `GoogleAiGeminiChatModel` bean

---

## Phases 5–8 — Agent Pipeline (Research → Content → Review → Image)

Each agent:
- `@Service` with typed Java record I/O
- Logs to `agent_logs` (running → success/failed)
- Increments `gemini_usage` per Gemini call
- Max 10 Gemini calls per pipeline run (enforced by orchestrator)

| Agent | Gemini calls | External API |
|-------|-------------|--------------|
| TopicResearchAgent | 1/topic (max 3) | Tavily Search |
| ContentCreationAgent | 1 | — |
| ReviewAgent | 2 (review + embeddings) | — |
| ImageAgent | 1 | Supabase Storage |

### Phase 5 — TopicResearchAgent ✅

- `TavilySearchClient` — search API wrapper
- `TopicResearchAgent` — researches up to 3 active topics per run
- Saves `research_data` rows (summary only, batch `saveAll()`)
- `ResearchOutput` record

### Phase 6 — ContentCreationAgent ✅

- `PostContent` / `GeminiPostContent` / `ContentCreationOutput` records
- `PostContentAssembler` — assembles LinkedIn `full_text` format
- `ContentCreationAgent` — 1 Gemini call per post
  - Input: `ResearchData` + last 5 `LearningPattern`s
  - Constraints: 150–300 words, 2–4 emojis, curiosity hook
  - Saves `generated_posts` with `status=draft`
- `run(userId, runId, researchId)` — single post
- `runForUnprocessedResearch(userId, runId)` — up to 3 posts from latest research
- Tests: `PostContentAssemblerTest`, `ContentCreationAgentTest`

### Phase 7 — ReviewAgent ✅

- `ReadabilityUtil` — Flesch Reading Ease → 0–25 readability sub-score (pure Java)
- `CosineSimilarityUtil` — embedding similarity for originality check
- `ReviewAgent` — 2 Gemini calls per run (combined review + embedding batch)
  - `grammar_clarity` + `technical_accuracy` from Gemini (0–25 each)
  - `readability` from Flesch-Kincaid (0–25)
  - `originality` from cosine similarity vs last 10 posts (0–25, flag if >0.85)
  - Threshold ≥85 → `approved`, else `rejected` + `quality_feedback`
- `GeminiRateLimiter.embedBatch()` — rate-limited embedding calls
- Tests: `ReadabilityUtilTest`, `CosineSimilarityUtilTest`, `ReviewAgentTest`

### Phase 8 — ImageAgent ✅

- `GeminiImageClient` — multimodal Gemini REST API with `responseModalities: ["TEXT","IMAGE"]`
- `ImageResizeUtil` — resize PNG to 1080×1080 via Java ImageIO
- `SupabaseStorageClient` — upload/download with service key + `@Retry(name = "supabase")`
- `ImageAgent` — 1 Gemini call per post, saves `generated_images` with `public_url`
  - Prompt: title + 3 key points + brand footer (user full name)
  - Path: `images/{userId}/{postId}.png`
  - Requires `approved` post status
- `StorageException` for upload failures
- Tests: `ImageAgentTest`, `ImageResizeUtilTest`, `SupabaseStorageClientTest`

### Phase 9 — SchedulerAgent + Quartz ✅

- `ScheduleTimeUtil` — next slot from user timezone + `preferred_post_time`, same-day conflict skip
- `SchedulerAgent` — saves `scheduled_posts` (queued), updates post to `scheduled`, 0 Gemini calls
- `QuartzJobScheduler` — one-time `LinkedInPublishJob` per scheduled post
- `LinkedInPublishJob` + `LinkedInPublishJobHandler` — fires at `scheduledFor` (full publish in phase 10)
- `QuartzConfig` cron jobs:
  - `06:00 UTC` — `TopicResearchJob`
  - `07:00 UTC` — `ContentGenerationJob`
  - every hour — `AnalyticsFetchJob` (stub → phase 11)
  - Sunday `03:00 UTC` — `PurgeOldLogsJob` (90-day retention)
- Tests: `ScheduleTimeUtilTest`, `SchedulerAgentTest`, `QuartzJobSchedulerTest`

### Phase 10 — LinkedInPublishingAgent ✅

- `LinkedInApiClient` — register upload, binary upload, UGC post publish (`@Retry linkedin`)
- `LinkedInPublishingAgent` — 0 Gemini calls, full publish pipeline
- `NotificationService` — token expired + manual approval emails (Gmail SMTP)
- Posting modes:
  - `auto` — publish to LinkedIn immediately
  - `manual` — email approval link, reschedule +2h, skip if not approved
  - `draft` — local `published_posts` row only (no LinkedIn API)
- Error handling: 401 → clear token + email user; 429 → Resilience4j retry
- `PostAnalyticsJob` — scheduled at +24h, +72h, +7d (full fetch in phase 11)
- `approveManualPublish(userId, postId)` — for manual mode approval
- Tests: `LinkedInPublishingAgentTest`

---

## Phases 9–12 — Scheduling, Publishing, Analytics, Learning

- `SchedulerAgent` — Quartz `LinkedInPublishJob`
- `LinkedInPublishingAgent` — asset upload + UGC post
- `AnalyticsAgent` — hourly fetch at 24h/72h/7d windows
- `SelfLearningAgent` — pattern analysis → `learning_patterns`

---

## Phase 13 — Remaining REST API

Controllers: Dashboard, Topics, Posts, Schedule, Analytics, Settings, Agents, Usage.

---

## Phases 14–16 — React Frontend

- Vite + React 18 + TypeScript strict + Tailwind + Shadcn UI
- TanStack Query v5, Axios JWT interceptor, React Hook Form + Zod
- Pages: landing, auth, dashboard, posts, schedule, analytics, agents, settings

---

## Phase 17 — Tests + CI

- Backend: JUnit 5, Mockito, Testcontainers, WireMock (80% target)
- Frontend: Vitest, RTL, MSW (70% target)
- `.github/workflows/ci.yml` — Checkstyle, SpotBugs, tsc, eslint, tests

---

## Phase 18 — Docker + Deploy

- `infra/docker-compose.yml` — API + PostgreSQL (local dev)
- `infra/docker-compose.prod.yml` — production overrides
- `infra/nginx/` — reverse proxy config
- Vercel (frontend) + Railway/Render (backend JAR)

---

## Free-Tier Guardrails (apply in every phase)

1. Never exceed **12 Gemini req/min** (Resilience4j rate limiter)
2. Never exceed **1400 Gemini calls/day** (DB counter, reject at limit)
3. Max **10 Gemini calls per agent pipeline run**
4. Batch DB writes with `saveAll()` — no row-by-row inserts in loops
5. No `raw_content` in DB — summaries only
6. Purge `agent_logs` older than 90 days (weekly Quartz job)
7. All secrets in `.env.example` only — never commit real keys

---

## Definition of Done (v1)

- [x] `docker-compose up` starts all services with zero manual steps
- [ ] New user registers → connects LinkedIn → published post within 24h
- [x] `GeminiUsageBar` shows real call count vs 1500/day limit
- [x] Dashboard shows real LinkedIn analytics
- [x] All tests pass in CI
- [x] README setup works on fresh machine with zero paid services
