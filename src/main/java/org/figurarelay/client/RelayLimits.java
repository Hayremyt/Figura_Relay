package org.figurarelay.client;

/**
 * Avatar size limit. The server is authoritative when the relay is active;
 * otherwise a local default applies (clean Figura has no such config).
 */
public class RelayLimits {

    public static int maxAvatarSize = 100 * 1024; // 100KB default

    public static void setLimits() {
        if (RelayState.isRelayActive()) {
            maxAvatarSize = RelayState.getServerMaxAvatarSize();
        } else {
            maxAvatarSize = 100 * 1024;
        }
    }
}
