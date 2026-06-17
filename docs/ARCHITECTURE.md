# LinkedIn AI Agent — Architecture

> Multi-agent SaaS that researches topics, creates LinkedIn posts + images,
> schedules, publishes, tracks analytics, and self-improves.
> **Every design decision optimises for free-tier limits.**

---

## 1. High-Level System Diagram

```mermaid
graph TB
    subgraph "Frontend — Vercel Free"
        WEB["React 18 + Vite<br/>Tailwind + Shadcn UI"]
    end

    subgraph "Backend — Railway/Render Free"
        API["Spring Boot 3.3<br/>Java 21"]
        AGENTS["Agent Pipeline<br/>(8 Agents)"]
        QUARTZ["Quartz Scheduler<br/>(DB-backed, no Redis)"]
        SEC["Spring Security 6<br/>JWT + LinkedIn OAuth"]
    end

    subgraph "External Services — All Free Tier"
        GEMINI["Gemini 2.0 Flash<br/>15 RPM / 1500 req/day"]
        TAVILY["Tavily Search<br/>1000 req/month"]
        LINKEDIN["LinkedIn API<br/>OAuth + Publishing"]
        SMTP["Gmail SMTP<br/>Notifications"]
    end

    subgraph "Data Layer — Supabase Free"
        DB[("PostgreSQL<br/>500 MB")]
        STORAGE[("Supabase Storage<br/>1 GB")]
    end

    WEB -->|"REST + JWT"| API
    API --> AGENTS
    API --> QUARTZ
    API --> SEC
    AGENTS --> GEMINI
    AGENTS --> TAVILY
    AGENTS --> LINKEDIN
    AGENTS --> DB
    AGENTS --> STORAGE
    QUARTZ --> DB
    API --> DB
    API --> SMTP
```

---

## 2. Monorepo Structure

```
linkedin-ai-agent/
├── apps/
│   ├── web/                          # React 18 frontend (Vite)
│   │   ├── public/
│   │   ├── src/
│   │   │   ├── components/
│   │   │   │   ├── ui/               # Shadcn UI primitives
│   │   │   │   ├── dashboard/        # StatCard, GeminiUsageBar, AgentTimeline
│   │   │   │   ├── posts/            # PostCard, QualityScore, LinkedInPreview
│   │   │   │   ├── schedule/         # PostCalendar
│   │   │   │   ├── analytics/        # EngagementChart
│   │   │   │   ├── agents/           # AgentStatusBadge, AgentTimeline
│   │   │   │   ├── topics/           # TopicManager
│   │   │   │   └── layout/           # Navbar, Sidebar, ThemeToggle
│   │   │   ├── pages/                # Route-level components
│   │   │   ├── hooks/                # Custom hooks (useAuth, usePosts, etc.)
│   │   │   ├── lib/                  # axios instance, query client, utils
│   │   │   ├── types/                # Generated from OpenAPI + manual
│   │   │   ├── context/              # AuthContext, ThemeContext
│   │   │   └── App.tsx / main.tsx
│   │   ├── index.html
│   │   ├── vite.config.ts
│   │   ├── tailwind.config.ts
│   │   ├── tsconfig.json
│   │   └── package.json
│   │
│   └── api/                          # Spring Boot backend
│       ├── src/main/java/com/linkedinagent/
│       │   ├── LinkedinAgentApplication.java
│       │   ├── config/
│       │   │   ├── SecurityConfig.java
│       │   │   ├── QuartzConfig.java
│       │   │   ├── SpringAIConfig.java
│       │   │   ├── SupabaseConfig.java
│       │   │   ├── RateLimiterConfig.java
│       │   │   ├── JacksonConfig.java
│       │   │   └── CorsConfig.java
│       │   ├── controller/
│       │   │   ├── AuthController.java
│       │   │   ├── DashboardController.java
│       │   │   ├── TopicController.java
│       │   │   ├── PostController.java
│       │   │   ├── ScheduleController.java
│       │   │   ├── AnalyticsController.java
│       │   │   ├── SettingsController.java
│       │   │   ├── AgentController.java
│       │   │   └── UsageController.java
│       │   ├── service/
│       │   │   ├── AuthService.java
│       │   │   ├── TopicService.java
│       │   │   ├── PostService.java
│       │   │   ├── ScheduleService.java
│       │   │   ├── AnalyticsService.java
│       │   │   ├── UserService.java
│       │   │   └── GeminiUsageService.java
│       │   ├── agent/
│       │   │   ├── TopicResearchAgent.java
│       │   │   ├── ContentCreationAgent.java
│       │   │   ├── ImageAgent.java
│       │   │   ├── ReviewAgent.java
│       │   │   ├── SchedulerAgent.java
│       │   │   ├── LinkedInPublishingAgent.java
│       │   │   ├── AnalyticsAgent.java
│       │   │   ├── SelfLearningAgent.java
│       │   │   └── AgentPipelineOrchestrator.java
│       │   ├── job/
│       │   │   ├── TopicResearchJob.java
│       │   │   ├── ContentGenerationJob.java
│       │   │   ├── LinkedInPublishJob.java
│       │   │   ├── AnalyticsFetchJob.java
│       │   │   ├── PurgeOldLogsJob.java
│       │   │   └── SelfLearningJob.java
│       │   ├── repository/
│       │   │   ├── UserRepository.java
│       │   │   ├── TopicRepository.java
│       │   │   ├── ResearchDataRepository.java
│       │   │   ├── GeneratedPostRepository.java
│       │   │   ├── GeneratedImageRepository.java
│       │   │   ├── ScheduledPostRepository.java
│       │   │   ├── PublishedPostRepository.java
│       │   │   ├── AnalyticsRepository.java
│       │   │   ├── AgentLogRepository.java
│       │   │   ├── LearningPatternRepository.java
│       │   │   └── GeminiUsageRepository.java
│       │   ├── entity/
│       │   │   ├── User.java
│       │   │   ├── Topic.java
│       │   │   ├── ResearchData.java
│       │   │   ├── GeneratedPost.java
│       │   │   ├── GeneratedImage.java
│       │   │   ├── ScheduledPost.java
│       │   │   ├── PublishedPost.java
│       │   │   ├── Analytics.java
│       │   │   ├── AgentLog.java
│       │   │   ├── LearningPattern.java
│       │   │   └── GeminiUsage.java
│       │   ├── dto/
│       │   │   ├── request/            # RegisterRequest, LoginRequest, etc.
│       │   │   └── response/           # AuthResponse, DashboardSummary, etc.
│       │   ├── exception/
│       │   │   ├── AgentException.java
│       │   │   ├── LinkedInApiException.java
│       │   │   ├── StorageException.java
│       │   │   ├── RateLimitException.java
│       │   │   ├── BudgetExceededException.java
│       │   │   └── GlobalExceptionHandler.java
│       │   ├── security/
│       │   │   ├── JwtTokenProvider.java
│       │   │   ├── JwtAuthFilter.java
│       │   │   └── LinkedInOAuthHandler.java
│       │   └── util/
│       │       ├── ReadabilityUtil.java
│       │       ├── GeminiRateLimiter.java
│       │       ├── EncryptionUtil.java
│       │       └── SupabaseStorageClient.java
│       ├── src/main/resources/
│       │   ├── application.yml
│       │   ├── application-dev.yml
│       │   ├── application-prod.yml
│       │   └── db/migration/
│       │       ├── V1__init.sql
│       │       └── V2__quartz.sql
│       ├── src/test/java/com/linkedinagent/
│       │   ├── agent/                  # Agent unit tests
│       │   ├── controller/             # Controller integration tests
│       │   ├── service/                # Service unit tests
│       │   └── util/                   # Utility tests
│       ├── pom.xml
│       └── Dockerfile
│
├── packages/
│   └── shared-types/                   # OpenAPI-generated TS types
│       ├── package.json
│       └── src/
│
├── infra/
│   ├── docker-compose.yml
│   ├── docker-compose.prod.yml
│   └── nginx/
│       └── default.conf
│
├── scripts/
│   └── seed.sql
│
├── docs/
│   ├── ARCHITECTURE.md                 # (this file)
│   └── API.md
│
├── .env.example
├── .github/workflows/
│   ├── ci.yml
│   └── deploy.yml
├── .gitignore
└── README.md
```

---

## 3. Data Model (ER Diagram)

```mermaid
erDiagram
    users ||--o{ topics : owns
    users ||--o{ generated_posts : creates
    users ||--o{ scheduled_posts : schedules
    users ||--o{ published_posts : publishes
    users ||--o{ analytics : tracks
    users ||--o{ agent_logs : logs
    users ||--o{ learning_patterns : learns

    topics ||--o{ research_data : produces
    topics ||--o{ generated_posts : inspires

    research_data ||--o| generated_posts : feeds

    generated_posts ||--o| generated_images : has
    generated_posts ||--o| scheduled_posts : becomes

    scheduled_posts ||--o| published_posts : becomes
    published_posts ||--o{ analytics : measured_by

    users {
        uuid id PK
        string email UK
        string hashed_password
        string full_name
        text linkedin_access_token "AES-256 encrypted"
        string linkedin_profile_id
        string linkedin_profile_url
        string timezone "default UTC"
        string posting_mode "auto|manual|draft"
        time preferred_post_time "default 09:00"
        timestamptz created_at
        timestamptz updated_at
    }

    topics {
        uuid id PK
        uuid user_id FK
        string name
        string category "ai|software_engineering|java|spring_boot|cloud|system_design|career"
        boolean is_active "default true"
        int priority "default 5"
        timestamptz created_at
    }

    research_data {
        uuid id PK
        uuid topic_id FK
        string source_url
        string source_title
        text summary "Gemini-summarised, no raw content"
        jsonb key_concepts
        float relevance_score
        timestamptz fetched_at
    }

    generated_posts {
        uuid id PK
        uuid user_id FK
        uuid topic_id FK
        uuid research_id FK
        string title
        text hook
        text body
        jsonb key_takeaways
        string call_to_action
        text full_text
        int quality_score
        jsonb quality_feedback
        string status "draft|approved|rejected|scheduled|published"
        string rejection_reason
        int word_count
        timestamptz created_at
        timestamptz updated_at
    }

    generated_images {
        uuid id PK
        uuid post_id FK
        string storage_path
        string public_url
        text prompt_used
        int width "default 1080"
        int height "default 1080"
        timestamptz created_at
    }

    scheduled_posts {
        uuid id PK
        uuid post_id FK
        uuid image_id FK
        uuid user_id FK
        timestamptz scheduled_for
        string status "queued|processing|published|failed"
        int retry_count "default 0"
        text last_error
        timestamptz created_at
    }

    published_posts {
        uuid id PK
        uuid scheduled_post_id FK
        uuid user_id FK
        string linkedin_post_id UK
        string linkedin_post_url
        timestamptz published_at
    }

    analytics {
        uuid id PK
        uuid published_post_id FK
        uuid user_id FK
        int impressions
        int likes
        int comments
        int shares
        float engagement_rate
        timestamptz fetched_at
    }

    agent_logs {
        uuid id PK
        uuid user_id FK
        string agent_name
        uuid run_id
        string status "running|success|failed"
        text input_summary
        text output_summary
        text error_message
        bigint duration_ms
        timestamptz started_at
        timestamptz finished_at
    }

    learning_patterns {
        uuid id PK
        uuid user_id FK
        string pattern_type "success|failure"
        string topic_category
        jsonb content_features
        float avg_engagement_rate
        int sample_size
        text insight
        timestamptz created_at
    }

    gemini_usage {
        uuid id PK
        date usage_date UK
        int call_count "default 0"
        timestamptz last_updated
    }
```

---

## 4. Agent Pipeline Architecture

```mermaid
sequenceDiagram
    participant Q as Quartz Scheduler
    participant A1 as TopicResearchAgent
    participant A2 as ContentCreationAgent
    participant A3 as ImageAgent
    participant A4 as ReviewAgent
    participant A5 as SchedulerAgent
    participant A6 as LinkedInPublishingAgent
    participant A7 as AnalyticsAgent
    participant A8 as SelfLearningAgent
    participant G as Gemini 2.0 Flash
    participant T as Tavily Search
    participant LI as LinkedIn API
    participant DB as PostgreSQL
    participant S as Supabase Storage

    Note over Q: 06:00 UTC Daily
    Q->>A1: Trigger TopicResearchJob
    A1->>DB: Fetch 3 active topics
    A1->>T: Search each topic (3 calls)
    A1->>G: Summarise + extract concepts (3 calls)
    A1->>DB: Save research_data (batch)

    Note over Q: 07:00 UTC Daily (or chained)
    Q->>A2: Trigger ContentGenerationJob
    A2->>DB: Load research + learning_patterns
    A2->>G: Generate post (1 call)
    A2->>DB: Save generated_posts

    A2->>A3: Chain to ImageAgent
    A3->>G: Generate image (1 call, multimodal)
    A3->>S: Upload PNG to Supabase Storage
    A3->>DB: Save generated_images

    A3->>A4: Chain to ReviewAgent
    A4->>G: Grammar + accuracy check (1 call)
    A4->>G: Embeddings for similarity (1 call)
    A4->>DB: Update quality_score + status

    alt score >= 85
        A4->>A5: Chain to SchedulerAgent
        A5->>DB: Insert scheduled_posts
        A5->>Q: Create LinkedInPublishJob
    else score < 85
        A4->>DB: Mark rejected + feedback
    end

    Note over Q: At scheduled_for time
    Q->>A6: Trigger LinkedInPublishJob
    A6->>S: Download image
    A6->>LI: Register upload + publish
    A6->>DB: Save published_posts
    A6->>Q: Schedule analytics fetches

    Note over Q: Every 1 hour
    Q->>A7: Trigger AnalyticsFetchJob
    A7->>LI: Fetch share stats
    A7->>DB: Upsert analytics

    Note over Q: After 7-day window
    Q->>A8: Trigger SelfLearningJob
    A8->>DB: Load last 30 posts + analytics
    A8->>G: Pattern analysis (1 call)
    A8->>DB: Upsert learning_patterns
```

### Gemini Call Budget Per Full Pipeline Run

| Agent | Gemini Calls | Purpose |
|---|---|---|
| TopicResearchAgent | 3 | 1 per topic (max 3 topics) |
| ContentCreationAgent | 1 | Generate post content |
| ImageAgent | 1 | Multimodal image generation |
| ReviewAgent | 2 | Combined review + embeddings |
| SchedulerAgent | 0 | Pure DB + Quartz |
| LinkedInPublishingAgent | 0 | LinkedIn API only |
| AnalyticsAgent | 0 | LinkedIn API only |
| SelfLearningAgent | 1 | Pattern analysis |
| **Total** | **8** | **Well under 10/run, 1500/day** |

---

## 5. Authentication & Security Architecture

```mermaid
sequenceDiagram
    participant U as User/Browser
    participant F as React Frontend
    participant B as Spring Boot API
    participant DB as PostgreSQL
    participant LI as LinkedIn OAuth

    Note over U,F: Email/Password Flow
    U->>F: Register / Login
    F->>B: POST /api/v1/auth/login
    B->>DB: Verify credentials (BCrypt)
    B->>F: { accessToken (15min), refreshToken (7d, HTTP-only cookie) }
    F->>F: Store accessToken in memory

    Note over U,F: Token Refresh
    F->>B: Request with expired access token
    B->>F: 401 Unauthorized
    F->>B: POST /api/v1/auth/refresh (cookie)
    B->>F: New accessToken
    F->>B: Retry original request

    Note over U,LI: LinkedIn OAuth Flow
    U->>F: Click "Connect LinkedIn"
    F->>B: GET /api/v1/auth/linkedin/connect
    B->>U: Redirect to LinkedIn auth URL
    U->>LI: Authorise app
    LI->>B: GET /api/v1/auth/linkedin/callback?code=xxx
    B->>LI: Exchange code for token
    B->>DB: Store AES-256 encrypted token
    B->>F: Redirect to /settings?linkedin=connected
```

---

## 6. Free-Tier Constraint Map

| Service | Limit | Our Budget | Guard Mechanism |
|---|---|---|---|
| Gemini 2.0 Flash RPM | 15 RPM | 12 RPM | Resilience4j `RateLimiter` bean |
| Gemini 2.0 Flash Daily | 1500 req/day | 1400 req/day | `gemini_usage` table + `BudgetExceededException` |
| Gemini TPM | 1M TPM | ~50K per run | Prompt length limits in agents |
| Supabase DB | 500 MB | ~100 MB target | No raw content storage; summaries only; 90-day log purge |
| Supabase Storage | 1 GB | ~500 MB target | 1080×1080 PNG (~200KB each), ~2500 images |
| Supabase Bandwidth | 2 GB/month | ~1 GB target | Image CDN via public URLs, no re-downloads |
| Tavily Search | 1000 req/month | ~90/month | 3 topics × 30 days = 90 |
| Gmail SMTP | 500/day | ~5/day | Only critical notifications |

---

## 7. Resilience & Error Handling

```
┌─────────────────────────────────────────────┐
│            GlobalExceptionHandler           │
│         (@ControllerAdvice)                 │
│                                             │
│  AgentException        → 500 AGENT_ERROR    │
│  LinkedInApiException  → 502 LINKEDIN_ERROR │
│  StorageException      → 502 STORAGE_ERROR  │
│  RateLimitException    → 429 RATE_LIMITED   │
│  BudgetExceededException → 429 BUDGET_EXCEEDED│
│  MethodArgumentNotValid → 400 VALIDATION_ERROR│
│  AccessDeniedException → 403 FORBIDDEN      │
│  All others            → 500 INTERNAL_ERROR │
└─────────────────────────────────────────────┘

Resilience4j annotations on external calls:
  @Retry(name = "gemini",  maxAttempts = 3, waitDuration = 2s)
  @Retry(name = "linkedin", maxAttempts = 3, exponentialBackoff)
  @CircuitBreaker(name = "gemini", failureRateThreshold = 50)
  @RateLimiter(name = "gemini", limitForPeriod = 12, limitRefreshPeriod = 60s)
```

---

## 8. Frontend Architecture

```mermaid
graph TB
    subgraph "State Management"
        TQ["TanStack Query v5<br/>Server State Cache"]
        AC["AuthContext<br/>User + Token"]
        TC["ThemeContext<br/>Dark/Light"]
    end

    subgraph "Routing — React Router v6"
        PUB["Public: /, /login, /register"]
        PRO["Protected: /dashboard, /posts,<br/>/schedule, /analytics,<br/>/agents, /settings"]
    end

    subgraph "API Layer"
        AX["Axios Instance<br/>JWT interceptor<br/>Auto-refresh on 401"]
    end

    subgraph "UI Layer"
        SH["Shadcn UI Primitives"]
        FM["Framer Motion<br/>Page transitions"]
        RC["Recharts<br/>Analytics charts"]
    end

    PRO --> TQ
    TQ --> AX
    AX -->|"REST"| API["Spring Boot API"]
    PUB --> AC
    PRO --> AC
    SH --> FM
```

### Page → Component Map

| Page | Key Components |
|---|---|
| `/dashboard` | StatCard ×4, GeminiUsageBar, AgentTimeline, EngagementChart |
| `/posts` | PostCard list, QualityScore, LinkedInPreview, ContentEditor |
| `/schedule` | PostCalendar (weekly grid), time picker |
| `/analytics` | EngagementChart (Recharts Line), stat cards, insights list |
| `/agents` | AgentStatusBadge (pulse dot), AgentTimeline (vertical feed), log viewer |
| `/settings` | TopicManager (tag selector, max 7), LinkedIn connect, theme, posting mode |

---

## 9. Deployment Architecture

```mermaid
graph LR
    subgraph "Vercel Free"
        FE["React SPA<br/>Static files"]
    end

    subgraph "Railway Starter / Render Free"
        BE["Spring Boot JAR<br/>Java 21"]
    end

    subgraph "Supabase Free"
        PG[("PostgreSQL")]
        ST[("Object Storage")]
    end

    FE -->|HTTPS| BE
    BE -->|JDBC| PG
    BE -->|HTTP| ST

    subgraph "CI/CD — GitHub Actions"
        CI["PR: lint + test"]
        CD["main: build + deploy"]
    end

    CI --> CD
    CD --> FE
    CD --> BE
```

---

## 10. Key Design Decisions

| Decision | Rationale |
|---|---|
| No Redis | Quartz DB-backed scheduler eliminates Redis cost entirely |
| No raw content in DB | Supabase free tier is 500 MB; store Gemini summaries only |
| Single Gemini API key | Google AI Studio key works without GCP billing |
| 10-call pipeline cap | 8 actual calls per full run; leaves headroom for retries |
| `jsonb` for flexible fields | Avoids extra tables for key_concepts, quality_feedback, etc. |
| AES-256 for LinkedIn tokens | Encrypted at rest in DB, decrypted only at publish time |
| 90-day log purge | Weekly Quartz job keeps agent_logs table lean |
| Batch DB writes | `saveAll()` everywhere; never row-by-row in loops |
| Quartz PostgreSQL DDL via Flyway | Standard `V2__quartz.sql` keeps schema versioned |
| JWT access + refresh split | 15-min access for security, 7-day refresh for UX |
