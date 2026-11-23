package com.throttlix.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiKeyAuthServiceTest {

    private ApiKeyAuthService authService;

    @BeforeEach
    void setUp() {
        authService = new ApiKeyAuthService();
    }

    @Test
    void shouldReturnNullWhenApiKeyIsNull() {
        String clientId = authService.authenticate(null);
        assertNull(clientId);
    }

    @Test
    void shouldReturnNullWhenApiKeyIsEmptyOrWhitespace() {
        assertNull(authService.authenticate(""));
        assertNull(authService.authenticate("   "));
        assertNull(authService.authenticate("\t\n"));
    }

    @Test
    void shouldReturnNullWhenApiKeyIsInvalid() {
        assertNull(authService.authenticate("completely-invalid-key"));
        assertNull(authService.authenticate("unknown-key-999"));
    }

    @Test
    void shouldNotTreatRawKeyAsClientId() {
        assertNull(authService.authenticate("thx-unregistered-key"));
        assertNull(authService.authenticate("thx-some-fake-client"));
    }

    @Test
    void shouldAuthenticateValidDevelopmentKeyForAnalyticsClient() {
        String clientId = authService.authenticate("thx-test-key-1");
        assertNotNull(clientId);
        assertEquals("client-analytics", clientId);
        assertNotEquals("thx-test-key-1", clientId);
    }

    @Test
    void shouldAuthenticateValidDevelopmentKeyForMobileClient() {
        String clientId = authService.authenticate("thx-test-key-2");
        assertNotNull(clientId);
        assertEquals("client-mobile", clientId);
        assertNotEquals("thx-test-key-2", clientId);
    }

    @Test
    void shouldAuthenticateCustomRegisteredKey() {
        String rawKey = "custom-test-secret-key-123";
        String expectedClientId = "client-payment-gateway";

        authService.registerKey(rawKey, expectedClientId);

        String authenticatedClientId = authService.authenticate(rawKey);
        assertEquals(expectedClientId, authenticatedClientId);
        assertNotEquals(rawKey, authenticatedClientId);
    }

    @Test
    void shouldIgnoreNullOrBlankRegistration() {
        authService.registerKey(null, "client-x");
        authService.registerKey("key-x", null);
        authService.registerKey("", "client-x");
        authService.registerKey("key-y", "");

        assertNull(authService.authenticate("key-x"));
        assertNull(authService.authenticate("key-y"));
    }
}
