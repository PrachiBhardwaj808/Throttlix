package com.throttlix.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.throttlix.core.AlgorithmType;
import com.throttlix.core.RateLimitPolicy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class RateLimitPolicyDAOTest {

    private final RateLimitPolicyDAO policyDAO = new RateLimitPolicyDAO();

    @Test
    void mapsSlidingWindowPolicyAndConvertsSecondsToMillis() throws SQLException {
        ResultSet resultSet = singlePolicyRow(AlgorithmType.SLIDING_WINDOW.name());
        when(resultSet.getLong("window_seconds")).thenReturn(60L);
        PolicyQuery query = queryReturning(resultSet);

        RateLimitPolicy policy = policyDAO.findActivePolicy(query.connection(), "client-a").orElseThrow();

        assertEquals("7", policy.getPolicyId());
        assertEquals("client-a", policy.getClientId());
        assertEquals(AlgorithmType.SLIDING_WINDOW, policy.getAlgorithmType());
        assertEquals(100, policy.getLimit());
        assertEquals(60_000L, policy.getWindowDurationMillis());
        verify(query.statement()).setString(1, "client-a");
    }

    @Test
    void mapsTokenBucketRefillSettings() throws SQLException {
        ResultSet resultSet = singlePolicyRow(AlgorithmType.TOKEN_BUCKET.name());
        when(resultSet.getInt("refill_tokens")).thenReturn(5);
        when(resultSet.getLong("refill_duration_millis")).thenReturn(2_000L);
        PolicyQuery query = queryReturning(resultSet);

        RateLimitPolicy policy = policyDAO.findActivePolicy(query.connection(), "client-a").orElseThrow();

        assertEquals(AlgorithmType.TOKEN_BUCKET, policy.getAlgorithmType());
        assertEquals(100, policy.getLimit());
        assertEquals(5, policy.getRefillTokens());
        assertEquals(2_000L, policy.getRefillDurationMillis());
    }

    @Test
    void returnsEmptyWhenClientHasNoActivePolicy() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(false);
        PolicyQuery query = queryReturning(resultSet);

        assertTrue(policyDAO.findActivePolicy(query.connection(), "client-a").isEmpty());
    }

    @Test
    void rejectsMissingConnectionAndBlankClient() {
        assertThrows(IllegalArgumentException.class,
                () -> policyDAO.findActivePolicy(null, "client-a"));
        assertThrows(IllegalArgumentException.class,
                () -> policyDAO.findActivePolicy(mock(Connection.class), " "));
    }

    @Test
    void rejectsUnsupportedAlgorithmFromDatabase() throws SQLException {
        ResultSet resultSet = singlePolicyRow("UNKNOWN");
        PolicyQuery query = queryReturning(resultSet);

        assertThrows(SQLException.class,
                () -> policyDAO.findActivePolicy(query.connection(), "client-a"));
    }

    @Test
    void rejectsTokenBucketPolicyMissingRefillConfiguration() throws SQLException {
        ResultSet resultSet = singlePolicyRow(AlgorithmType.TOKEN_BUCKET.name());
        when(resultSet.getInt("refill_tokens")).thenReturn(0);
        when(resultSet.wasNull()).thenReturn(true);
        PolicyQuery query = queryReturning(resultSet);

        assertThrows(SQLException.class,
                () -> policyDAO.findActivePolicy(query.connection(), "client-a"));
    }

    private ResultSet singlePolicyRow(String algorithm) throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong("policy_id")).thenReturn(7L);
        when(resultSet.getString("client_name")).thenReturn("client-a");
        when(resultSet.getString("algorithm")).thenReturn(algorithm);
        when(resultSet.getInt("request_limit")).thenReturn(100);
        when(resultSet.getBoolean("enabled")).thenReturn(true);
        return resultSet;
    }

    private PolicyQuery queryReturning(ResultSet resultSet) throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        return new PolicyQuery(connection, statement);
    }

    private record PolicyQuery(Connection connection, PreparedStatement statement) { }
}
