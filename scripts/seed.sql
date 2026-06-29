-- Seed data for local development
-- Run against local PostgreSQL: psql -U postgres -d postgres -f scripts/seed.sql

-- This is a placeholder. In production, users register via the API.
-- For local testing, you can manually insert a test user:

-- INSERT INTO users (id, email, hashed_password, full_name, timezone, posting_mode, preferred_post_time, created_at, updated_at)
-- VALUES (
--   gen_random_uuid(),
--   'test@example.com',
--   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- password: password123
--   'Test User',
--   'UTC',
--   'draft',
--   '09:00:00',
--   NOW(),
--   NOW()
-- );

SELECT 'Seed script ready. Uncomment the INSERT statement above to create a test user.' AS info;
