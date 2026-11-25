package com.throttlix.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.throttlix.model.Client;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ClientDAOTest {

    private final ClientDAO clientDAO = new ClientDAO();

    @Test
    void shouldCreateClientAndReturnGeneratedKey() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet generatedKeys = mock(ResultSet.class);

        when(connection.prepareStatement(anyString(), anyInt())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        when(statement.getGeneratedKeys()).thenReturn(generatedKeys);
        when(generatedKeys.next()).thenReturn(true);
        when(generatedKeys.getLong(1)).thenReturn(101L);

        assertEquals(101L, clientDAO.createClient(connection, "client-enterprise"));
    }

    @Test
    void shouldFindClientByName() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong("client_id")).thenReturn(1L);
        when(resultSet.getString("name")).thenReturn("client-analytics");
        when(resultSet.getBoolean("active")).thenReturn(true);
        when(resultSet.getTimestamp("created_at")).thenReturn(new Timestamp(System.currentTimeMillis()));

        Optional<Client> client = clientDAO.findClientByName(connection, "client-analytics");
        assertTrue(client.isPresent());
        assertEquals(1L, client.get().clientId());
        assertEquals("client-analytics", client.get().name());
        assertTrue(client.get().active());
        assertNotNull(client.get().createdAt());
    }

    @Test
    void shouldCheckIfClientIsActive() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBoolean("active")).thenReturn(true);

        Optional<Boolean> active = clientDAO.isClientActive(connection, "client-mobile");
        assertTrue(active.isPresent());
        assertTrue(active.get());
    }

    @Test
    void shouldRejectNullOrBlankClientName() {
        Connection connection = mock(Connection.class);
        assertThrows(IllegalArgumentException.class, () -> clientDAO.createClient(null, "valid"));
        assertThrows(IllegalArgumentException.class, () -> clientDAO.createClient(connection, "   "));
        assertThrows(IllegalArgumentException.class, () -> clientDAO.findClientByName(connection, ""));
        assertThrows(IllegalArgumentException.class, () -> clientDAO.isClientActive(connection, null));
    }

    @Test
    void shouldUpdateClientStatus() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);

        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);

        boolean updated = clientDAO.setClientActive(connection, 1L, false);
        assertTrue(updated);
    }
}
