package org.figurarelay.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Persistent ban list (figura-banned-players.json, same file as the old relay). */
public class BanStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path BAN_FILE = Path.of("figura-banned-players.json");
    private static final Set<UUID> bannedPlayers = ConcurrentHashMap.newKeySet();

    public static void load() {
        if (!Files.exists(BAN_FILE)) {
            save();
            return;
        }
        try {
            bannedPlayers.clear();
            String json = Files.readString(BAN_FILE);
            String[] uuids = GSON.fromJson(json, String[].class);
            if (uuids != null) {
                for (String uuid : uuids) {
                    try {
                        bannedPlayers.add(UUID.fromString(uuid));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            RelayMain.LOGGER.info("Relay: loaded {} banned players", bannedPlayers.size());
        } catch (IOException e) {
            RelayMain.LOGGER.error("Relay: failed to load banned players", e);
        }
    }

    public static void save() {
        try {
            String json = GSON.toJson(bannedPlayers.stream().map(UUID::toString).toArray());
            Files.writeString(BAN_FILE, json);
        } catch (IOException e) {
            RelayMain.LOGGER.error("Relay: failed to save banned players", e);
        }
    }

    public static boolean isBanned(UUID playerId) {
        return bannedPlayers.contains(playerId);
    }

    public static void ban(UUID playerId) {
        bannedPlayers.add(playerId);
        save();
    }

    public static void unban(UUID playerId) {
        bannedPlayers.remove(playerId);
        save();
    }

    public static Set<UUID> getBanned() {
        return Set.copyOf(bannedPlayers);
    }
}
