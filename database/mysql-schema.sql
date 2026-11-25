-- ============================================================================
-- Throttlix MySQL Database Schema
-- Database: MySQL 8.x | Storage Engine: InnoDB | Character Set: utf8mb4
-- ============================================================================

CREATE DATABASE IF NOT EXISTS throttlix
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE throttlix;

-- ----------------------------------------------------------------------------
-- 1. Clients Table
-- Stores registered organizations or services using the rate-limiting gateway.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS clients (
    client_id   BIGINT NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (client_id),
    UNIQUE KEY uk_clients_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- 2. API Keys Table
-- Stores SHA-256 hashed API keys. Raw API keys are never stored for security.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS api_keys (
    api_key_id  BIGINT NOT NULL AUTO_INCREMENT,
    client_id   BIGINT NOT NULL,
    name        VARCHAR(100) NOT NULL,
    key_hash    VARCHAR(64) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at  TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (api_key_id),
    UNIQUE KEY uk_api_keys_hash (key_hash),
    FOREIGN KEY (client_id) REFERENCES clients (client_id) ON DELETE CASCADE,
    INDEX idx_api_keys_hash_active (key_hash, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- 3. Rate Limit Policies Table
-- Stores traffic rules for Token Bucket and Sliding Window algorithms.
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
    PRIMARY KEY (policy_id),
    UNIQUE KEY uk_policies_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- 4. Client Policies Junction Table
-- Maps each client to their assigned rate-limiting policy.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS client_policies (
    client_id    BIGINT NOT NULL,
    policy_id    BIGINT NOT NULL,
    assigned_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (client_id),
    FOREIGN KEY (client_id) REFERENCES clients (client_id) ON DELETE CASCADE,
    FOREIGN KEY (policy_id) REFERENCES rate_limit_policies (policy_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
