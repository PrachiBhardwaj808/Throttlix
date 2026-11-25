package com.throttlix.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConfig {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConfig.class.getName());

    public static final String DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";
    public static final String DEFAULT_DB_URL = "jdbc:mysql://localhost:3306/throttlix";
    public static final String DEFAULT_DB_USERNAME = "root";

    private final String url;
    private final String username;
    private final String password;

    static {
        try {
            Class.forName(DRIVER_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "MySQL JDBC Driver class not found: " + DRIVER_CLASS_NAME, e);
        }
    }

    public DatabaseConfig() {
        Properties properties = loadProperties();

        String envUrl = System.getenv("DB_URL");
        String envUser = System.getenv("DB_USERNAME");
        String envPass = System.getenv("DB_PASSWORD");

        this.url = (envUrl != null && !envUrl.isBlank())
                ? envUrl.trim()
                : properties.getProperty("db.url", DEFAULT_DB_URL);

        this.username = (envUser != null && !envUser.isBlank())
                ? envUser.trim()
                : properties.getProperty("db.username", DEFAULT_DB_USERNAME);

        this.password = (envPass != null)
                ? envPass
                : properties.getProperty("db.password", "");
    }

    public DatabaseConfig(String url, String username, String password) {
        this.url = (url != null && !url.isBlank()) ? url : DEFAULT_DB_URL;
        this.username = (username != null && !username.isBlank()) ? username : DEFAULT_DB_USERNAME;
        this.password = (password != null) ? password : "";
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load application.properties", e);
        }
        return props;
    }

    public Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, () -> "Database connection failure to: " + getMaskedUrl());
            throw new SQLException("Failed to establish database connection to: " + getMaskedUrl(), e);
        }
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getDriverClassName() {
        return DRIVER_CLASS_NAME;
    }

    public String getMaskedUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "DatabaseConfig[driver=" + DRIVER_CLASS_NAME + ", url=" + url + ", username=" + username + ", password=***]";
    }
}
