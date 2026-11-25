-- ============================================================================
-- Throttlix MySQL 8.x Relational Database Schema
-- Storage Engine: InnoDB | Character Set: utf8mb4 | Collation: utf8mb4_unicode_ci
-- ============================================================================

-- Create Database if not exists
CREATE DATABASE IF NOT EXISTS throttlix
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE throttlix;

-- ----------------------------------------------------------------------------
-- 1. Clients Table
-- Stores registered organizations, services, or consumers of the gateway.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS clients (
    client_id   BIGINT NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_clients PRIMARY KEY (client_id),
    CONSTRAINT uk_clients_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 2. API Keys Table
-- Stores cryptographic SHA-256 hashes of client credentials. Raw secrets are
-- never persisted. Supports active, rotating, and revoked keys.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS api_keys (
    api_key_id  BIGINT NOT NULL AUTO_INCREMENT,
    client_id   BIGINT NOT NULL,
    name        VARCHAR(100) NOT NULL,
    key_hash    VARCHAR(64) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at  TIMESTAMP NULL DEFAULT NULL,
    CONSTRAINT pk_api_keys PRIMARY KEY (api_key_id),
    CONSTRAINT uk_api_keys_hash UNIQUE (key_hash),
    CONSTRAINT fk_api_keys_client FOREIGN KEY (client_id)
        REFERENCES clients (client_id) ON DELETE CASCADE,
    INDEX idx_api_keys_hash_active (key_hash, active),
    INDEX idx_api_keys_client_id (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 3. Rate Limit Policies Table
-- Stores reusable traffic policies supporting both TOKEN_BUCKET and SLIDING_WINDOW.
-- Token Bucket uses refill_tokens and refill_duration_millis.
-- Sliding Window uses request_limit and window_seconds.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rate_limit_policies (
    policy_id               BIGINT NOT NULL AUTO_INCREMENT,
    name                    VARCHAR(100) NOT NULL,
    algorithm               VARCHAR(30) NOT NULL,
    request_limit           INT NOT NULL,
    window_seconds          INT NOT NULL,
    refill_tokens           INT NULL DEFAULT NULL,
    refill_duration_millis  BIGINT NULL DEFAULT NULL,
    enabled                 BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_rate_limit_policies PRIMARY KEY (policy_id),
    CONSTRAINT uk_rate_limit_policies_name UNIQUE (name),
    CONSTRAINT chk_policy_algorithm CHECK (algorithm IN ('TOKEN_BUCKET', 'SLIDING_WINDOW')),
    CONSTRAINT chk_policy_request_limit CHECK (request_limit > 0),
    CONSTRAINT chk_policy_window_seconds CHECK (window_seconds > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 4. Client Policies Junction Table
-- Maps each client account to their active rate-limiting policy.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS client_policies (
    client_id    BIGINT NOT NULL,
    policy_id    BIGINT NOT NULL,
    assigned_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_client_policies PRIMARY KEY (client_id),
    CONSTRAINT fk_client_policies_client FOREIGN KEY (client_id)
        REFERENCES clients (client_id) ON DELETE CASCADE,
    CONSTRAINT fk_client_policies_policy FOREIGN KEY (policy_id)
        REFERENCES rate_limit_policies (policy_id) ON DELETE RESTRICT,
    INDEX idx_client_policies_policy_id (policy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 5. Request Telemetry & Audit Logs Table
-- Append-only audit trail logging gateway traffic decisions, latency, and status.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS request_logs (
    log_id        BIGINT NOT NULL AUTO_INCREMENT,
    client_id     BIGINT NULL DEFAULT NULL,
    api_key_id    BIGINT NULL DEFAULT NULL,
    endpoint      VARCHAR(255) NOT NULL,
    http_method   VARCHAR(10) NOT NULL,
    status_code   INT NOT NULL,
    allowed       BOOLEAN NOT NULL,
    latency_ms    INT NOT NULL,
    requested_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_request_logs PRIMARY KEY (log_id),
    CONSTRAINT fk_request_logs_client FOREIGN KEY (client_id)
        REFERENCES clients (client_id) ON DELETE SET NULL,
    CONSTRAINT fk_request_logs_api_key FOREIGN KEY (api_key_id)
        REFERENCES api_keys (api_key_id) ON DELETE SET NULL,
    INDEX idx_request_logs_client_time (client_id, requested_at DESC),
    INDEX idx_request_logs_analytics (requested_at DESC, allowed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
