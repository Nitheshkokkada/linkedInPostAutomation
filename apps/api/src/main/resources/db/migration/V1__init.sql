-- =============================================
-- V1__init.sql — LinkedIn AI Agent schema
-- All 11 application tables + indexes
-- =============================================

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================
-- 1. users
-- =============================================
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    hashed_password VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255),
    linkedin_access_token   TEXT,
    linkedin_profile_id     VARCHAR(255),
    linkedin_profile_url    VARCHAR(500),
    timezone        VARCHAR(50) NOT NULL DEFAULT 'UTC',
    posting_mode    VARCHAR(20) NOT NULL DEFAULT 'draft'
                    CHECK (posting_mode IN ('auto', 'manual', 'draft')),
    preferred_post_time TIME NOT NULL DEFAULT '09:00:00',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email ON users(email);

-- =============================================
-- 2. topics
-- =============================================
CREATE TABLE topics (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    category    VARCHAR(50) NOT NULL
                CHECK (category IN ('ai', 'software_engineering', 'java', 'spring_boot', 'cloud', 'system_design', 'career')),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    priority    INTEGER NOT NULL DEFAULT 5,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_topics_user_id ON topics(user_id);
CREATE INDEX idx_topics_user_active ON topics(user_id, is_active);

-- =============================================
-- 3. research_data
-- NOTE: No raw_content column — Gemini summaries only to stay within 500MB free tier
-- =============================================
CREATE TABLE research_data (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id        UUID NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    source_url      VARCHAR(2000),
    source_title    VARCHAR(500),
    summary         TEXT NOT NULL,
    key_concepts    JSONB,
    relevance_score FLOAT,
    fetched_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_research_topic_id ON research_data(topic_id);

-- =============================================
-- 4. generated_posts
-- =============================================
CREATE TABLE generated_posts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    topic_id            UUID REFERENCES topics(id) ON DELETE SET NULL,
    research_id         UUID REFERENCES research_data(id) ON DELETE SET NULL,
    title               VARCHAR(500),
    hook                TEXT,
    body                TEXT,
    key_takeaways       JSONB,
    call_to_action      TEXT,
    full_text           TEXT,
    quality_score       INTEGER,
    quality_feedback    JSONB,
    status              VARCHAR(20) NOT NULL DEFAULT 'draft'
                        CHECK (status IN ('draft', 'approved', 'rejected', 'scheduled', 'published')),
    rejection_reason    TEXT,
    word_count          INTEGER,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_posts_user_id ON generated_posts(user_id);
CREATE INDEX idx_posts_user_status ON generated_posts(user_id, status);

-- =============================================
-- 5. generated_images
-- =============================================
CREATE TABLE generated_images (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id         UUID NOT NULL REFERENCES generated_posts(id) ON DELETE CASCADE,
    storage_path    VARCHAR(1000),
    public_url      VARCHAR(2000),
    prompt_used     TEXT,
    width           INTEGER NOT NULL DEFAULT 1080,
    height          INTEGER NOT NULL DEFAULT 1080,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_images_post_id ON generated_images(post_id);

-- =============================================
-- 6. scheduled_posts
-- =============================================
CREATE TABLE scheduled_posts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id         UUID NOT NULL REFERENCES generated_posts(id) ON DELETE CASCADE,
    image_id        UUID REFERENCES generated_images(id) ON DELETE SET NULL,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    scheduled_for   TIMESTAMPTZ NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'queued'
                    CHECK (status IN ('queued', 'processing', 'published', 'failed')),
    retry_count     INTEGER NOT NULL DEFAULT 0,
    last_error      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_scheduled_user_id ON scheduled_posts(user_id);
CREATE INDEX idx_scheduled_status ON scheduled_posts(status, scheduled_for);

-- =============================================
-- 7. published_posts
-- =============================================
CREATE TABLE published_posts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scheduled_post_id   UUID NOT NULL REFERENCES scheduled_posts(id) ON DELETE CASCADE,
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    linkedin_post_id    VARCHAR(255) UNIQUE,
    linkedin_post_url   VARCHAR(2000),
    published_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_published_user_id ON published_posts(user_id);

-- =============================================
-- 8. analytics
-- =============================================
CREATE TABLE analytics (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    published_post_id   UUID NOT NULL REFERENCES published_posts(id) ON DELETE CASCADE,
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    impressions         INTEGER NOT NULL DEFAULT 0,
    likes               INTEGER NOT NULL DEFAULT 0,
    comments            INTEGER NOT NULL DEFAULT 0,
    shares              INTEGER NOT NULL DEFAULT 0,
    engagement_rate     FLOAT NOT NULL DEFAULT 0.0,
    fetched_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_analytics_published_post ON analytics(published_post_id);
CREATE INDEX idx_analytics_user_id ON analytics(user_id);

-- =============================================
-- 9. agent_logs
-- =============================================
CREATE TABLE agent_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID REFERENCES users(id) ON DELETE SET NULL,
    agent_name      VARCHAR(100) NOT NULL,
    run_id          UUID NOT NULL,
    status          VARCHAR(20) NOT NULL
                    CHECK (status IN ('running', 'success', 'failed')),
    input_summary   TEXT,
    output_summary  TEXT,
    error_message   TEXT,
    duration_ms     BIGINT,
    started_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at     TIMESTAMPTZ
);

CREATE INDEX idx_agent_logs_user_started ON agent_logs(user_id, started_at DESC);
CREATE INDEX idx_agent_logs_run_id ON agent_logs(run_id);

-- =============================================
-- 10. learning_patterns
-- =============================================
CREATE TABLE learning_patterns (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    pattern_type        VARCHAR(20) NOT NULL
                        CHECK (pattern_type IN ('success', 'failure')),
    topic_category      VARCHAR(50),
    content_features    JSONB,
    avg_engagement_rate FLOAT NOT NULL DEFAULT 0.0,
    sample_size         INTEGER NOT NULL DEFAULT 0,
    insight             TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_patterns_user_id ON learning_patterns(user_id);

-- =============================================
-- 11. gemini_usage
-- =============================================
CREATE TABLE gemini_usage (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usage_date   DATE NOT NULL UNIQUE,
    call_count   INTEGER NOT NULL DEFAULT 0,
    last_updated TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_gemini_usage_date ON gemini_usage(usage_date);
