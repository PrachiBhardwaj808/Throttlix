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
        loadDevelopmentKeys();
    }

    private void loadDevelopmentKeys() {
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

        String cleanKey = apiKey.trim();
        if (cleanKey.isEmpty()) {
            LOGGER.warning("Authentication failed: empty API key");
            return null;
        }

        // 1. Try to find the client in the MySQL database
        if (apiKeyDAO != null && databaseConfig != null) {
            try (Connection connection = databaseConfig.getConnection()) {
                String keyHash = hashKey(cleanKey);
                Optional<String> clientId = apiKeyDAO.findClientByActiveKeyHash(connection, keyHash);
                if (clientId.isPresent()) {
                    String resolvedClient = clientId.get();
                    LOGGER.info(() -> "Authentication successful via MySQL for client: " + resolvedClient);
                    return resolvedClient;
                }
            } catch (SQLException e) {
                // If database is unreachable, log warning and fall back to local test keys
                LOGGER.log(Level.WARNING, () -> "MySQL authentication unavailable: " + e.getMessage());
            }
        }

        // 2. Check in-memory development keys
        String inMemoryClient = keyToClientMap.get(cleanKey);
        if (inMemoryClient != null) {
            LOGGER.info(() -> "Authentication successful (in-memory) for client: " + inMemoryClient);
            return inMemoryClient;
        }

        LOGGER.warning("Authentication failed: invalid or revoked API key");
        return null;
    }

    public void registerKey(String apiKey, String clientId) {
        if (apiKey != null && clientId != null) {
            String cleanKey = apiKey.trim();
            String cleanClient = clientId.trim();
            if (!cleanKey.isEmpty() && !cleanClient.isEmpty()) {
                keyToClientMap.put(cleanKey, cleanClient);
            }
        }
    }

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
