package com.throttlix.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.throttlix.model.Client;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
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
        Connection connection = mockConnectionWithGeneratedKey(101L);
        long clientId = clientDAO.createClient(connection, "client-enterprise");
        assertEquals(101L, clientId);
    }

    @Test
    void shouldFindClientByName() throws Exception {
        Connection connection = mockConnectionWithClientRow(1L, "client-analytics", true);
        Optional<Client> client = clientDAO.findClientByName(connection, "client-analytics");
        assertTrue(client.isPresent());
        assertEquals(1L, client.get().clientId());
        assertEquals("client-analytics", client.get().name());
        assertTrue(client.get().active());
        assertNotNull(client.get().createdAt());
    }

    @Test
    void shouldCheckIfClientIsActive() throws Exception {
        Connection connection = mockConnectionWithActiveStatus(true);
        Optional<Boolean> active = clientDAO.isClientActive(connection, "client-mobile");
        assertTrue(active.isPresent());
        assertTrue(active.get());
    }

    @Test
    void shouldRejectNullOrBlankClientName() {
        Connection connection = mockBasicConnection();
        assertThrows(IllegalArgumentException.class, () -> clientDAO.createClient(null, "valid"));
        assertThrows(IllegalArgumentException.class, () -> clientDAO.createClient(connection, "   "));
        assertThrows(IllegalArgumentException.class, () -> clientDAO.findClientByName(connection, ""));
        assertThrows(IllegalArgumentException.class, () -> clientDAO.isClientActive(connection, null));
    }

    @Test
    void shouldUpdateClientStatus() throws Exception {
        Connection connection = mockConnectionWithUpdateCount(1);
        boolean updated = clientDAO.setClientActive(connection, 1L, false);
        assertTrue(updated);
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private Connection mockBasicConnection() {
        return proxy(Connection.class, (p, m, a) -> null);
    }

    private Connection mockConnectionWithGeneratedKey(long key) {
        ResultSet keysRs = proxy(ResultSet.class, (p, m, a) -> switch (m.getName()) {
            case "next" -> true;
            case "getLong" -> key;
            case "close" -> null;
            default -> null;
        });

        PreparedStatement stmt = proxy(PreparedStatement.class, (p, m, a) -> switch (m.getName()) {
            case "setString" -> null;
            case "executeUpdate" -> 1;
            case "getGeneratedKeys" -> keysRs;
            case "close" -> null;
            default -> null;
        });

        return proxy(Connection.class, (p, m, a) -> {
            if ("prepareStatement".equals(m.getName())) return stmt;
            return null;
        });
    }

    private Connection mockConnectionWithClientRow(long id, String name, boolean active) {
        ResultSet rs = proxy(ResultSet.class, (p, m, a) -> switch (m.getName()) {
            case "next" -> true;
            case "getLong" -> id;
            case "getString" -> name;
            case "getBoolean" -> active;
            case "getTimestamp" -> new Timestamp(System.currentTimeMillis());
            case "close" -> null;
            default -> null;
        });

        PreparedStatement stmt = proxy(PreparedStatement.class, (p, m, a) -> switch (m.getName()) {
            case "setString", "setLong" -> null;
            case "executeQuery" -> rs;
            case "close" -> null;
            default -> null;
        });

        return proxy(Connection.class, (p, m, a) -> {
            if ("prepareStatement".equals(m.getName())) return stmt;
            return null;
        });
    }

    private Connection mockConnectionWithActiveStatus(boolean active) {
        ResultSet rs = proxy(ResultSet.class, (p, m, a) -> switch (m.getName()) {
            case "next" -> true;
            case "getBoolean" -> active;
            case "close" -> null;
            default -> null;
        });

        PreparedStatement stmt = proxy(PreparedStatement.class, (p, m, a) -> switch (m.getName()) {
            case "setString" -> null;
            case "executeQuery" -> rs;
            case "close" -> null;
            default -> null;
        });

        return proxy(Connection.class, (p, m, a) -> {
            if ("prepareStatement".equals(m.getName())) return stmt;
            return null;
        });
    }

    private Connection mockConnectionWithUpdateCount(int count) {
        PreparedStatement stmt = proxy(PreparedStatement.class, (p, m, a) -> switch (m.getName()) {
            case "setBoolean", "setLong" -> null;
            case "executeUpdate" -> count;
            case "close" -> null;
            default -> null;
        });

        return proxy(Connection.class, (p, m, a) -> {
            if ("prepareStatement".equals(m.getName())) return stmt;
            return null;
        });
    }
}
