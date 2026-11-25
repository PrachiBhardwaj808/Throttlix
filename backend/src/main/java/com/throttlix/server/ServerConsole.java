package com.throttlix.server;

import java.io.IOException;

/**
 * Terminal 1: Throttlix Gateway Server Console.
 * Starts the live Gateway HTTP Server and displays real-time request authentication,
 * rate limit evaluation, upstream forwarding, and response telemetry.
 */
public class ServerConsole {

    public static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException ignored) {
                // Keep default port
            }
        } else {
            String envPort = System.getenv("PORT");
            if (envPort != null && !envPort.isBlank()) {
                try {
                    port = Integer.parseInt(envPort.trim());
                } catch (NumberFormatException ignored) {
                    // Keep default port
                }
            }
        }

        GatewayServer server = new GatewayServer(port);

        // Register shutdown hook for clean termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SHUTDOWN] Shutting down Throttlix Gateway Server...");
            server.stop(0);
        }));

        try {
            server.start();

            // Keep the main thread alive while the server runs
            Thread.currentThread().join();
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to start Gateway Server on port " + port + ": " + e.getMessage());
            System.err.println("Ensure port " + port + " is not already in use by another application.");
        } catch (InterruptedException e) {
            server.stop(0);
            Thread.currentThread().interrupt();
        }
    }
}
