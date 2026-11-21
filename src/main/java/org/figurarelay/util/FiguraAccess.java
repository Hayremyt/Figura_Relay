package org.figurarelay.util;

import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.UserData;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Reflection access to Figura's private AvatarManager registries. */
public class FiguraAccess {

    private static final Field LOADED_USERS_FIELD;
    private static final Field FETCHED_USERS_FIELD;

    static {
        try {
            LOADED_USERS_FIELD = AvatarManager.class.getDeclaredField("LOADED_USERS");
            LOADED_USERS_FIELD.setAccessible(true);
            FETCHED_USERS_FIELD = AvatarManager.class.getDeclaredField("FETCHED_USERS");
            FETCHED_USERS_FIELD.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Figura Relay: failed to access AvatarManager fields", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<UUID, UserData> loadedUsers() {
        try {
            return (Map<UUID, UserData>) LOADED_USERS_FIELD.get(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Set<UUID> fetchedUsers() {
        try {
            return (Set<UUID>) FETCHED_USERS_FIELD.get(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
