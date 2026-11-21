package org.figurarelay.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side relay state. Client only (references Figura).
 */
public class RelayState {

    private static boolean relayActive = false;
    private static int serverMaxAvatarSize = 0;

    public static boolean isRelayActive() { return relayActive; }
    public static int getServerMaxAvatarSize() { return serverMaxAvatarSize; }

    /** Reset state when joining a new server; the platform sender transmits the Hello. */
    public static void onClientJoinServer() {
        relayActive = false;
        serverMaxAvatarSize = 0;
        lastAvatarRequest.clear();
        org.figurarelay.util.LastHashStore.clear();
        org.figuramc.figura.backend2.NetworkStuff.backendStatus = 1;
        org.figuramc.figura.backend2.NetworkStuff.setLimits();
        org.figurarelay.FiguraRelay.LOGGER.info("Relay: joined server, sending Hello");
        RelaySender.sendHello();
    }

    /** Reset state on disconnect so nothing stale lingers in the main menu. */
    public static void onDisconnect() {
        if (!relayActive && serverMaxAvatarSize == 0) return;
        relayActive = false;
        serverMaxAvatarSize = 0;
        clearAll();
        org.figuramc.figura.backend2.NetworkStuff.backendStatus = 1;
        org.figuramc.figura.backend2.NetworkStuff.setLimits();
    }

    /** Called when Hello is received from server — relay is active. */
    public static void onServerHello(int maxAvatarSize) {
        relayActive = true;
        serverMaxAvatarSize = maxAvatarSize;
        org.figuramc.figura.backend2.NetworkStuff.backendStatus = 0;
        org.figuramc.figura.backend2.NetworkStuff.setLimits();
        org.figurarelay.FiguraRelay.LOGGER.info("Relay: server relay active, maxAvatarSize={}", maxAvatarSize);
    }

    /** Called when RelayOff is received — server disabled relay for this player. */
    public static void onRelayOff() {
        relayActive = false;
        serverMaxAvatarSize = 0;
        clearAll();
        org.figuramc.figura.backend2.NetworkStuff.backendStatus = 1;
        org.figuramc.figura.backend2.NetworkStuff.setLimits();
        org.figurarelay.FiguraRelay.LOGGER.info("Relay: server relay disabled (banned)");
    }

    // -- rate-limited avatar requests (retry fallback, never floods) -- //

    private static final Map<UUID, Long> lastAvatarRequest = new ConcurrentHashMap<>();
    private static final long AVATAR_REQUEST_COOLDOWN_MS = 1000;

    public static boolean requestAvatar(UUID targetId) {
        long now = System.currentTimeMillis();
        Long lastReq = lastAvatarRequest.get(targetId);
        if (lastReq != null && now - lastReq < AVATAR_REQUEST_COOLDOWN_MS) {
            return false;
        }
        lastAvatarRequest.put(targetId, now);
        RelaySender.requestAvatar(targetId);
        return true;
    }

    public static void clearRequest(UUID targetId) {
        lastAvatarRequest.remove(targetId);
    }

    public static void clearAll() {
        lastAvatarRequest.clear();
        org.figurarelay.util.LastHashStore.clear();
    }
}
