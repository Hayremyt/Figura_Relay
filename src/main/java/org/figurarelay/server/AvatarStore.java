package org.figurarelay.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * File-based avatar storage, indexed by player UUID and hash.
 * Same on-disk layout as the old relay (figura-avatars/, figura-player-hashes.json)
 * so existing data migrates by just copying the files next to the server.
 */
public class AvatarStore {

    private static Path storagePath;
    private static final Path HASH_FILE = Path.of("figura-player-hashes.json");
    private static final Map<UUID, String> playerHashes = new ConcurrentHashMap<>();

    public static void init() {
        storagePath = Path.of("figura-avatars");
        try {
            Files.createDirectories(storagePath);
            if (RelayConfig.isLogOperations())
                RelayMain.LOGGER.debug("Relay: avatar storage at {}", storagePath.toAbsolutePath());

            // Restore player→hash mapping so avatars survive server restarts
            boolean hasMapping = false;
            if (Files.exists(HASH_FILE)) {
                String json = Files.readString(HASH_FILE);
                var map = new com.google.gson.Gson().fromJson(json,
                        com.google.gson.reflect.TypeToken.getParameterized(java.util.Map.class, String.class, String.class).getType());
                if (map != null) {
                    for (var entry : ((java.util.Map<String, String>) map).entrySet()) {
                        try {
                            playerHashes.put(UUID.fromString(entry.getKey()), entry.getValue());
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
                hasMapping = true;
                if (RelayConfig.isLogOperations())
                    RelayMain.LOGGER.debug("Relay: restored {} player hashes", playerHashes.size());
            }

            // Garbage-collect orphan avatar files (no player references them anymore).
            // Only when a hash mapping exists — without it every file would look orphaned.
            if (hasMapping) {
                File[] files = storagePath.toFile().listFiles((dir, name) -> name.endsWith(".nbt"));
                if (files != null) {
                    int removed = 0;
                    for (File file : files) {
                        String name = file.getName();
                        String hash = name.substring(0, name.length() - 4);
                        if (playerHashes.containsValue(hash))
                            continue;
                        try {
                            Files.deleteIfExists(file.toPath());
                            removed++;
                        } catch (IOException e) {
                            RelayMain.LOGGER.error("Relay: failed to remove orphaned avatar file {}", name, e);
                        }
                    }
                    if (removed > 0 && RelayConfig.isLogOperations())
                        RelayMain.LOGGER.info("Relay: removed {} orphaned avatar file(s)", removed);
                }
            }
        } catch (IOException e) {
            RelayMain.LOGGER.error("Relay: failed to initialize avatar storage", e);
        }
    }

    private static void saveHashes() {
        try {
            var map = new java.util.HashMap<String, String>();
            for (var entry : playerHashes.entrySet())
                map.put(entry.getKey().toString(), entry.getValue());
            Files.writeString(HASH_FILE, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(map));
        } catch (IOException e) {
            RelayMain.LOGGER.error("Relay: failed to save player hashes", e);
        }
    }

    public static void save(UUID playerId, String hash, CompoundTag nbt) {
        String oldHash = playerHashes.put(playerId, hash);
        saveHashes();
        if (nbt == null) return;

        if (oldHash != null && !oldHash.equals(hash)) {
            boolean stillReferenced = false;
            for (String h : playerHashes.values()) {
                if (oldHash.equals(h)) { stillReferenced = true; break; }
            }
            if (!stillReferenced) {
                Path oldFilePath = storagePath.resolve(oldHash + ".nbt");
                try {
                    Files.deleteIfExists(oldFilePath);
                    if (RelayConfig.isLogOperations())
                        RelayMain.LOGGER.debug("Relay: removed old avatar for {} (old hash: {})", playerId, oldHash);
                } catch (IOException e) {
                    RelayMain.LOGGER.error("Relay: failed to remove old avatar for {}", playerId, e);
                }
            }
        }

        Path filePath = storagePath.resolve(hash + ".nbt");
        try (OutputStream os = Files.newOutputStream(filePath)) {
            NbtIo.writeCompressed(nbt, os);
            if (RelayConfig.isLogOperations())
                RelayMain.LOGGER.debug("Relay: saved avatar for {} (hash: {})", playerId, hash);
        } catch (IOException e) {
            RelayMain.LOGGER.error("Relay: failed to save avatar for {}", playerId, e);
        }
    }

    public static CompoundTag load(UUID playerId) {
        String hash = playerHashes.get(playerId);
        if (hash == null) return null;

        Path filePath = storagePath.resolve(hash + ".nbt");
        if (!Files.exists(filePath)) {
            RelayMain.LOGGER.warn("Relay: avatar file missing for {} (hash: {})", playerId, hash);
            return null;
        }

        try (InputStream is = Files.newInputStream(filePath)) {
            return NbtIo.readCompressed(is, NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            RelayMain.LOGGER.error("Relay: failed to load avatar for {}", playerId, e);
            return null;
        }
    }

    public static void remove(UUID playerId) {
        String hash = playerHashes.remove(playerId);
        saveHashes();
        if (hash == null) return;

        for (String h : playerHashes.values()) {
            if (hash.equals(h)) return;
        }

        Path filePath = storagePath.resolve(hash + ".nbt");
        try {
            Files.deleteIfExists(filePath);
            if (RelayConfig.isLogOperations())
                RelayMain.LOGGER.debug("Relay: removed avatar for {} (hash: {})", playerId, hash);
        } catch (IOException e) {
            RelayMain.LOGGER.error("Relay: failed to remove avatar for {}", playerId, e);
        }
    }

    public static String getPlayerHash(UUID playerId) {
        return playerHashes.get(playerId);
    }

    public static Map<UUID, String> getAllHashes() {
        return new ConcurrentHashMap<>(playerHashes);
    }

    public static Path getStoragePath() {
        return storagePath;
    }
}
