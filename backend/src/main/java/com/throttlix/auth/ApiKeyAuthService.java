package com.throttlix.auth;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

// TEMPORARY: Development-only in-memory storage.
// In production, persistent storage (PostgreSQL) and cryptographic key hashing (SHA-256)
// will be used via ClientDao/ApiKeyDao without hardcoding or storing raw keys in code.
public class ApiKeyAuthService {

    private static final Logger LOGGER = Logger.getLogger(ApiKeyAuthService.class.getName());

    // TEMPORARY: In-memory store for development and testing.
    private final ConcurrentMap<String, String> keyToClientMap = new ConcurrentHashMap<>();

    public ApiKeyAuthService() {
        initializeDevelopmentKeys();
    }

    private void initializeDevelopmentKeys() {
        keyToClientMap.put("thx-test-key-1", "client-analytics");
        keyToClientMap.put("thx-test-key-2", "client-mobile");
        keyToClientMap.put("dev-token-bucket-key", "client-analytics");
        keyToClientMap.put("dev-sliding-window-key", "client-mobile");
    }

    public String authenticate(String apiKey) {
        if (apiKey == null) {
            LOGGER.warning("Authentication failed: missing API key");
            return null;
        }

        String trimmedKey = apiKey.trim();
        if (trimmedKey.isEmpty()) {
            LOGGER.warning("Authentication failed: empty API key");
            return null;
        }

        String clientId = keyToClientMap.get(trimmedKey);
        if (clientId != null) {
            LOGGER.info(() -> "Authentication successful for client ID: " + clientId);
            return clientId;
        }

        LOGGER.warning("Authentication failed: invalid API key");
        return null;
    }

    public void registerKey(String apiKey, String clientId) {
        if (apiKey != null && clientId != null) {
            String trimmedKey = apiKey.trim();
            String trimmedClient = clientId.trim();
            if (!trimmedKey.isEmpty() && !trimmedClient.isEmpty()) {
                keyToClientMap.put(trimmedKey, trimmedClient);
            }
        }
    }
}
