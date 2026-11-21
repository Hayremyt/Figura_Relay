package org.figurarelay.client;

import net.minecraft.client.Minecraft;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.UserData;
import org.figuramc.figura.avatar.local.CacheAvatarLoader;
import org.figuramc.figura.backend2.NetworkStuff;
import org.figurarelay.util.FiguraAccess;
import org.figurarelay.util.LastHashStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-tick relay maintenance. Replaces Figura's backend tick().
 * Invoked from NetworkStuffMixin. Client only.
 */
public class RelayTick {

    private static final List<UUID> SUBSCRIPTIONS = new ArrayList<>();

    public static void tick() {
        RelayUpload.tickRates();
        tickSubscriptions();
        tickPingCounter();
        cacheCheck();
    }

    /** Tracks online players locally (no backend subscribe). */
    private static void tickSubscriptions() {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            if (!SUBSCRIPTIONS.isEmpty()) {
                SUBSCRIPTIONS.clear();
            }
            org.figurarelay.client.RelayState.onDisconnect();
            return;
        }

        List<UUID> unsub = new ArrayList<>(SUBSCRIPTIONS);
        for (UUID uuid : connection.getOnlinePlayerIds()) {
            unsub.remove(uuid);
            if (!SUBSCRIPTIONS.contains(uuid)) {
                SUBSCRIPTIONS.add(uuid);
                // Same guard as the old subscribe(): ignore non-v4 UUIDs.
                if (uuid.version() == 4) {
                    FiguraAccess.loadedUsers().computeIfAbsent(uuid, UserData::new);
                }
            }
        }

        SUBSCRIPTIONS.removeAll(unsub);
    }

    private static void tickPingCounter() {
        if (NetworkStuff.lastPing > 0 && FiguraMod.ticks - NetworkStuff.lastPing >= 20)
            NetworkStuff.lastPing = NetworkStuff.pingsSent = NetworkStuff.pingsReceived = 0;
    }

    /**
     * Fallback in case a hash broadcast was missed: load from local cache
     * or re-request avatar data from the server.
     */
    private static void cacheCheck() {
        if (Minecraft.getInstance().level == null) return;

        for (Map.Entry<UUID, UserData> entry : FiguraAccess.loadedUsers().entrySet()) {
            UUID ownerId = entry.getKey();
            UserData userData = entry.getValue();

            UUID localId = FiguraMod.getLocalPlayerUUID();
            if (localId != null && ownerId.equals(localId)) continue;

            String lastHash = LastHashStore.get(ownerId);
            if (lastHash == null || lastHash.isEmpty()) continue;

            boolean found = CacheAvatarLoader.checkAndLoad(lastHash, userData);
            if (found) {
                FiguraAccess.fetchedUsers().add(ownerId);
                continue;
            }

            Avatar mainAvatar = userData.getMainAvatar();
            if (mainAvatar == null || !mainAvatar.loaded || mainAvatar.nbt == null) {
                RelayUpload.getAvatar(userData, ownerId, "", lastHash);
            }
        }
    }
}
