package com.throttlix.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.throttlix.core.AlgorithmType;
import com.throttlix.core.RateLimitPolicy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RateLimitPolicyDAOTest {

    private final RateLimitPolicyDAO policyDAO = new RateLimitPolicyDAO();

    @Test
    void mapsSlidingWindowPolicyAndConvertsSecondsToMillis() throws SQLException {
        PolicyQuery query = queryReturning(row(AlgorithmType.SLIDING_WINDOW.name(), 60L, null, null));

        RateLimitPolicy policy = policyDAO.findActivePolicy(query.connection(), "client-a").orElseThrow();

        assertEquals("7", policy.getPolicyId());
        assertEquals("client-a", policy.getClientId());
        assertEquals(AlgorithmType.SLIDING_WINDOW, policy.getAlgorithmType());
        assertEquals(100, policy.getLimit());
        assertEquals(60_000L, policy.getWindowDurationMillis());
        assertEquals("client-a", query.statement().clientId);
    }

    @Test
    void mapsTokenBucketRefillSettings() throws SQLException {
        PolicyQuery query = queryReturning(row(AlgorithmType.TOKEN_BUCKET.name(), 1L, 5, 2_000L));

        RateLimitPolicy policy = policyDAO.findActivePolicy(query.connection(), "client-a").orElseThrow();

        assertEquals(AlgorithmType.TOKEN_BUCKET, policy.getAlgorithmType());
        assertEquals(100, policy.getLimit());
        assertEquals(5, policy.getRefillTokens());
        assertEquals(2_000L, policy.getRefillDurationMillis());
    }

    @Test
    void returnsEmptyWhenClientHasNoActivePolicy() throws SQLException {
        PolicyQuery query = queryReturning(null);

        assertTrue(policyDAO.findActivePolicy(query.connection(), "client-a").isEmpty());
    }

    @Test
    void rejectsMissingConnectionAndBlankClient() {
        assertThrows(IllegalArgumentException.class,
                () -> policyDAO.findActivePolicy(null, "client-a"));
        assertThrows(IllegalArgumentException.class,
                () -> policyDAO.findActivePolicy(queryReturning(null).connection(), " "));
    }

    @Test
    void rejectsUnsupportedAlgorithmFromDatabase() throws SQLException {
        PolicyQuery query = queryReturning(row("UNKNOWN", 60L, null, null));

        assertThrows(SQLException.class,
                () -> policyDAO.findActivePolicy(query.connection(), "client-a"));
    }

    @Test
    void rejectsTokenBucketPolicyMissingRefillConfiguration() throws SQLException {
        PolicyQuery query = queryReturning(row(AlgorithmType.TOKEN_BUCKET.name(), 1L, null, null));

        assertThrows(SQLException.class,
                () -> policyDAO.findActivePolicy(query.connection(), "client-a"));
    }

    @Test
    void shouldCreateSlidingWindowPolicyAndReturnGeneratedKey() throws SQLException {
        ResultSet keysRs = proxy(ResultSet.class, (p, m, a) -> switch (m.getName()) {
            case "next" -> true;
            case "getLong" -> 88L;
            case "close" -> null;
            default -> null;
        });

        PreparedStatement stmt = proxy(PreparedStatement.class, (p, m, a) -> switch (m.getName()) {
            case "setString", "setInt", "setNull", "setBoolean" -> null;
            case "executeUpdate" -> 1;
            case "getGeneratedKeys" -> keysRs;
            case "close" -> null;
            default -> null;
        });

        Connection connection = proxy(Connection.class, (p, m, a) -> {
            if ("prepareStatement".equals(m.getName())) return stmt;
            return null;
        });

        RateLimitPolicy policy = RateLimitPolicy.createSlidingWindowPolicy("pol-sw", "client-sw", 100, 60_000L);
        long policyId = policyDAO.createPolicy(connection, "pol-sw", policy);
        assertEquals(88L, policyId);
    }

    @Test
    void shouldAssignPolicyToClient() throws SQLException {
        PreparedStatement stmt = proxy(PreparedStatement.class, (p, m, a) -> switch (m.getName()) {
            case "setLong" -> null;
            case "executeUpdate" -> 1;
            case "close" -> null;
            default -> null;
        });

        Connection connection = proxy(Connection.class, (p, m, a) -> {
            if ("prepareStatement".equals(m.getName())) return stmt;
            return null;
        });

        boolean assigned = policyDAO.assignPolicyToClient(connection, 1L, 88L);
        assertTrue(assigned);
    }

    @Test
    void shouldUpdatePolicy() throws SQLException {
        PreparedStatement stmt = proxy(PreparedStatement.class, (p, m, a) -> switch (m.getName()) {
            case "setInt", "setLong", "setNull" -> null;
            case "executeUpdate" -> 1;
            case "close" -> null;
            default -> null;
        });

        Connection connection = proxy(Connection.class, (p, m, a) -> {
            if ("prepareStatement".equals(m.getName())) return stmt;
            return null;
        });

        RateLimitPolicy policy = RateLimitPolicy.createTokenBucketPolicy("pol-tb", "client-tb", 200, 10, 1000L);
        boolean updated = policyDAO.updatePolicy(connection, 88L, policy);
        assertTrue(updated);
    }

    private Map<String, Object> row(String algorithm, long windowSeconds,
                                    Integer refillTokens, Long refillDurationMillis) {
        Map<String, Object> values = new HashMap<>();
        values.put("policy_id", 7L);
        values.put("client_name", "client-a");
        values.put("algorithm", algorithm);
        values.put("request_limit", 100);
        values.put("window_seconds", windowSeconds);
        values.put("refill_tokens", refillTokens);
        values.put("refill_duration_millis", refillDurationMillis);
        values.put("enabled", true);
        return values;
    }

    private PolicyQuery queryReturning(Map<String, Object> row) {
        QueryState state = new QueryState(row);
        ResultSet resultSet = proxy(ResultSet.class, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "next" -> {
                    boolean hasRow = state.row != null && !state.read;
                    state.read = true;
                    yield hasRow;
                }
                case "getLong", "getInt", "getString", "getBoolean" -> {
                    Object value = state.row == null ? null : state.row.get(args[0]);
                    state.wasNull = value == null;
                    yield value == null ? defaultValue(method.getReturnType()) : value;
                }
                case "wasNull" -> state.wasNull;
                case "close" -> null;
                default -> defaultValue(method.getReturnType());
            };
        });
        PreparedStatement statement = proxy(PreparedStatement.class, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "setString" -> {
                    state.clientId = (String) args[1];
                    yield null;
                }
                case "executeQuery" -> {
                    yield resultSet;
                }
                case "close" -> null;
                default -> defaultValue(method.getReturnType());
            };
        });
        Connection connection = proxy(Connection.class, (proxy, method, args) -> {
            if (method.getName().equals("prepareStatement")) {
                state.sql = (String) args[0];
                return statement;
            }
            return defaultValue(method.getReturnType());
        });
        return new PolicyQuery(connection, state);
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
    }

    private Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }

    private static class QueryState {
        private final Map<String, Object> row;
        private boolean read;
        private boolean wasNull;
        private String clientId;
        private String sql;

        private QueryState(Map<String, Object> row) {
            this.row = row;
        }
    }

    private record PolicyQuery(Connection connection, QueryState statement) { }
}
