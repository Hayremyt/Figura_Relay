package org.figurarelay.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Server-side configuration. Lives in config/figura-relay.properties
 * (same keys as the old figura-server-relay.properties for easy migration).
 * Created with defaults on first run; reloaded via "/figura relay reload".
 */
public class RelayConfig {

    private static int maxAvatarSize = 102400; // 100KB default
    private static boolean logOperations = false;

    public static int getMaxAvatarSize() { return maxAvatarSize; }
    public static boolean isLogOperations() { return logOperations; }

    public static void setLogOperations(boolean value) {
        logOperations = value;
    }

    public static String getStoragePath() {
        return AvatarStore.getStoragePath().toString();
    }

    public static void load() {
        Path configDir = Path.of("config");
        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir);
            } catch (IOException e) {
                RelayMain.LOGGER.error("Relay: failed to create config directory", e);
                return;
            }
        }

        Path configPath = configDir.resolve("figura-relay.properties");
        Path legacyPath = configDir.resolve("figura-server-relay.properties");
        if (!Files.exists(configPath) && Files.exists(legacyPath)) {
            configPath = legacyPath;
            RelayMain.LOGGER.info("Relay: using legacy config at {}", legacyPath.toAbsolutePath());
        }
        if (!Files.exists(configPath)) {
            configPath = configDir.resolve("figura-relay.properties");
            writeDefaults(configPath);
        }

        try {
            Properties props = new Properties();
            try (var is = Files.newInputStream(configPath)) {
                props.load(is);
            }

            maxAvatarSize = Integer.parseInt(props.getProperty("maxAvatarSize", "102400"));
            logOperations = Boolean.parseBoolean(props.getProperty("logOperations", "false"));

            RelayMain.LOGGER.info("Relay: config loaded: maxAvatarSize={}, logOperations={}", maxAvatarSize, logOperations);
        } catch (Exception e) {
            RelayMain.LOGGER.error("Relay: failed to load config, using defaults", e);
        }
    }

    private static void writeDefaults(Path configPath) {
        try {
            String defaults = """
                    # Figura Relay Relay Configuration
                    # Max avatar size in bytes (default: 102400 = 100KB)
                    maxAvatarSize=102400
                    # Enable logging of relay operations (default: false)
                    logOperations=false
                    """;
            Files.writeString(configPath, defaults);
            RelayMain.LOGGER.info("Relay: created default config at {}", configPath.toAbsolutePath());
        } catch (IOException e) {
            RelayMain.LOGGER.error("Relay: failed to write default config", e);
        }
    }
}
