package com.throttlix.auth;

import com.throttlix.dao.ApiKeyDAO;
import com.throttlix.database.DatabaseConfig;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Authentication service responsible for verifying API keys.
 * Uses ApiKeyDAO to verify SHA-256 key hashes against MySQL,
 * falling back to in-memory development keys when database is offline or unpopulated.
 */
public class ApiKeyAuthService {

    private static final Logger LOGGER = Logger.getLogger(ApiKeyAuthService.class.getName());

    private final ApiKeyDAO apiKeyDAO;
    private final DatabaseConfig databaseConfig;
    private final ConcurrentMap<String, String> keyToClientMap = new ConcurrentHashMap<>();

    public ApiKeyAuthService() {
        this(new ApiKeyDAO(), new DatabaseConfig());
    }

    public ApiKeyAuthService(ApiKeyDAO apiKeyDAO, DatabaseConfig databaseConfig) {
        this.apiKeyDAO = apiKeyDAO;
        this.databaseConfig = databaseConfig;
        initializeDevelopmentKeys();
    }

    private void initializeDevelopmentKeys() {
        keyToClientMap.put("thx-test-key-1", "client-analytics");
        keyToClientMap.put("thx-test-key-2", "client-mobile");
        keyToClientMap.put("dev-token-bucket-key", "client-analytics");
        keyToClientMap.put("dev-sliding-window-key", "client-mobile");
    }

    /**
     * Authenticates an API key.
     * 1. Checks validity of input format.
     * 2. Hashes key using SHA-256 and queries MySQL via ApiKeyDAO.
     * 3. If MySQL query succeeds, returns mapped client ID.
     * 4. If key is not in DB or DB is offline, checks in-memory test map.
     * 5. Returns null if key is invalid, missing, or revoked.
     *
     * @param apiKey raw API key from X-API-Key header
     * @return client ID if authenticated; null otherwise
     */
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

        // 1. Try MySQL Database Authentication via ApiKeyDAO
        if (apiKeyDAO != null && databaseConfig != null) {
            try (Connection connection = databaseConfig.getConnection()) {
                String keyHash = hashKey(trimmedKey);
                Optional<String> clientId = apiKeyDAO.findClientByActiveKeyHash(connection, keyHash);
                if (clientId.isPresent()) {
                    String resolvedClient = clientId.get();
                    LOGGER.info(() -> "Authentication successful via MySQL for client ID: " + resolvedClient);
                    return resolvedClient;
                }
            } catch (SQLException e) {
                // Safe degradation: never leak SQL exceptions, never approve request on DB error
                LOGGER.log(Level.WARNING, () -> "MySQL authentication unavailable or connection failed (" + e.getMessage() + ")");
            }
        }

        // 2. Check development/test in-memory map
        String inMemoryClient = keyToClientMap.get(trimmedKey);
        if (inMemoryClient != null) {
            LOGGER.info(() -> "Authentication successful (in-memory) for client ID: " + inMemoryClient);
            return inMemoryClient;
        }

        LOGGER.warning("Authentication failed: invalid or revoked API key");
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

    /**
     * Computes the SHA-256 hexadecimal digest for an API key.
     * Ensures raw keys are never passed to persistent storage.
     *
     * @param rawKey the raw API key secret
     * @return 64-character lowercase hexadecimal hash string
     */
    public static String hashKey(String rawKey) {
        if (rawKey == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in JVM", e);
        }
    }
}
