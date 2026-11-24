

-- 1. Clients Table
CREATE TABLE IF NOT EXISTS clients (
    client_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. API Keys Table
CREATE TABLE IF NOT EXISTS api_keys (
    api_key_id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL REFERENCES clients(client_id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    key_hash VARCHAR(64) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_api_keys_hash 
    ON api_keys(key_hash) WHERE active = TRUE;

-- 3. Rate Limit Policies Table
CREATE TABLE IF NOT EXISTS rate_limit_policies (
    policy_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    algorithm VARCHAR(30) NOT NULL CHECK (algorithm IN ('TOKEN_BUCKET', 'SLIDING_WINDOW')),
    request_limit INTEGER NOT NULL CHECK (request_limit > 0),
    window_seconds INTEGER NOT NULL CHECK (window_seconds > 0),
    refill_tokens INTEGER,
    refill_duration_millis BIGINT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE rate_limit_policies
    ADD COLUMN IF NOT EXISTS refill_tokens INTEGER;

ALTER TABLE rate_limit_policies
    ADD COLUMN IF NOT EXISTS refill_duration_millis BIGINT;

-- 4. Client Policies Junction Table
CREATE TABLE IF NOT EXISTS client_policies (
    client_id BIGINT PRIMARY KEY REFERENCES clients(client_id) ON DELETE CASCADE,
    policy_id BIGINT NOT NULL REFERENCES rate_limit_policies(policy_id) ON DELETE RESTRICT,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Request Telemetry & Audit Logs Table
CREATE TABLE IF NOT EXISTS request_logs (
    log_id BIGSERIAL PRIMARY KEY,
    client_id BIGINT REFERENCES clients(client_id) ON DELETE SET NULL,
    api_key_id BIGINT REFERENCES api_keys(api_key_id) ON DELETE SET NULL,
    endpoint VARCHAR(255) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    status_code INTEGER NOT NULL,
    allowed BOOLEAN NOT NULL,
    latency_ms INTEGER NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_request_logs_client_time 
    ON request_logs (client_id, requested_at DESC);

CREATE INDEX IF NOT EXISTS idx_request_logs_analytics 
    ON request_logs (requested_at DESC, allowed);
