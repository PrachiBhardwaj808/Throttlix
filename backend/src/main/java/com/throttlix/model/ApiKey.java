package com.throttlix.model;

import java.time.Instant;

/**
 * Domain model representing a client API key.
 * Strictly holds cryptographic SHA-256 hashes rather than raw secret values.
 *
 * @param apiKeyId  unique identifier of the key
 * @param clientId  associated client ID foreign key
 * @param name      label or description for the key
 * @param keyHash   64-character SHA-256 hexadecimal digest
 * @param active    whether the key is currently authorized
 * @param createdAt timestamp of key issuance
 * @param revokedAt timestamp of key revocation (null if active)
 */
public record ApiKey(
        long apiKeyId,
        long clientId,
        String name,
        String keyHash,
        boolean active,
        Instant createdAt,
        Instant revokedAt
) {}
