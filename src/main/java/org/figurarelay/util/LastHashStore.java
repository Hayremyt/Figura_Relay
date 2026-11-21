package org.figurarelay.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** External store for the last-known avatar hash per player (replaces UserDataMixin field). */
public class LastHashStore {

    private static final Map<UUID, String> MAP = new ConcurrentHashMap<>();

    public static String get(UUID ownerId) {
        return MAP.getOrDefault(ownerId, "");
    }

    public static void set(UUID ownerId, String hash) {
        if (hash == null || hash.isEmpty()) MAP.remove(ownerId);
        else MAP.put(ownerId, hash);
    }

    public static void remove(UUID ownerId) {
        MAP.remove(ownerId);
    }

    public static void clear() {
        MAP.clear();
    }
}
