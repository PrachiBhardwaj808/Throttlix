package com.throttlix.database;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseConfigTest {

    @Test
    void shouldLoadDriverClassSuccessfully() throws Exception {
        assertEquals("com.mysql.cj.jdbc.Driver", DatabaseConfig.DRIVER_CLASS_NAME);
        Class<?> driverClass = Class.forName(DatabaseConfig.DRIVER_CLASS_NAME);
        assertNotNull(driverClass);
    }

    @Test
    void shouldInitializeWithDefaultMySQLConfiguration() {
        DatabaseConfig config = new DatabaseConfig();
        assertTrue(config.getUrl().startsWith("jdbc:mysql://"));
        assertEquals("com.mysql.cj.jdbc.Driver", config.getDriverClassName());
        assertNotNull(config.getUsername());
    }

    @Test
    void shouldMaskPasswordInToStringRepresentation() {
        DatabaseConfig config = new DatabaseConfig("jdbc:mysql://localhost:3306/throttlix", "test_user", "super_secret_pw");
        String str = config.toString();
        assertTrue(str.contains("password=***"));
        assertFalse(str.contains("super_secret_pw"));
        assertEquals("jdbc:mysql://localhost:3306/throttlix", config.getUrl());
        assertEquals("test_user", config.getUsername());
    }

    @Test
    void shouldThrowSQLExceptionWithMaskedDetailsWhenConnectionFails() {
        DatabaseConfig badConfig = new DatabaseConfig("jdbc:mysql://localhost:65530/throttlix", "test_user", "secret_pass_123");
        SQLException exception = assertThrows(SQLException.class, badConfig::getConnection);
        assertTrue(exception.getMessage().contains("Failed to establish database connection"));
        assertFalse(exception.getMessage().contains("secret_pass_123"));
    }

    @Test
    void shouldReturnFalseWhenTestConnectionFails() {
        DatabaseConfig badConfig = new DatabaseConfig("jdbc:mysql://localhost:65530/throttlix", "test_user", "secret_pass_123");
        assertFalse(badConfig.testConnection());
    }
}
