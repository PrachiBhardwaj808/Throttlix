package com.throttlix.model;

import java.time.Instant;

/**
 * Domain model representing a registered client organization or tenant.
 *
 * @param clientId unique identifier of the client (primary key)
 * @param name     unique client identifier name
 * @param active   whether the client account is currently active
 * @param createdAt timestamp when the client was registered
 */
public record Client(
        long clientId,
        String name,
        boolean active,
        Instant createdAt
) {}
