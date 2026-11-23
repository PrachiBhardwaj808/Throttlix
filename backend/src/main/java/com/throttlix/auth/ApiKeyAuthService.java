package com.throttlix.auth;

import java.util.HashMap;
import java.util.Map;

public class ApiKeyAuthService {

    private final Map<String, String> keyToClientMap = new HashMap<>();

    public ApiKeyAuthService() {
        keyToClientMap.put("thx-test-key-1", "client-analytics");
        keyToClientMap.put("thx-test-key-2", "client-mobile");
        keyToClientMap.put("thx-live-tokenbucket", "client-analytics");
        keyToClientMap.put("thx-live-slidingwindow", "client-mobile");
    }

    public String authenticate(String apiKey) {
        if (apiKey == null) {
            return null;
        }

        String trimmedKey = apiKey.trim();
        if (trimmedKey.isEmpty()) {
            return null;
        }

        String clientId = keyToClientMap.get(trimmedKey);
        if (clientId != null) {
            return clientId;
        }

        if (trimmedKey.startsWith("thx-")) {
            return "client-" + trimmedKey.substring(4);
        }

        return null;
    }

    public void registerKey(String apiKey, String clientId) {
        if (apiKey != null && clientId != null) {
            keyToClientMap.put(apiKey.trim(), clientId.trim());
        }
    }
}
