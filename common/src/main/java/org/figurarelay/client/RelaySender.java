package org.figurarelay.client;

import java.util.UUID;

/**
 * Outgoing relay packets. The platform modules (fabric/forge/neoforge)
 * install the real sender; before that a no-op keeps everything safe.
 */
public class RelaySender {

    public interface Sender {
        void sendAvatarHash(UUID ownerId, String hash);
        void requestAvatar(UUID targetId);
        void sendAvatarData(UUID ownerId, byte[] data);
        void sendPing(UUID ownerId, int pingId, byte[] data, boolean sync);
        void sendHello();
    }

    private static Sender sender = new Sender() {
        @Override public void sendAvatarHash(UUID ownerId, String hash) {}
        @Override public void requestAvatar(UUID targetId) {}
        @Override public void sendAvatarData(UUID ownerId, byte[] data) {}
        @Override public void sendPing(UUID ownerId, int pingId, byte[] data, boolean sync) {}
        @Override public void sendHello() {}
    };

    public static void init(Sender s) {
        sender = s;
    }

    public static void sendAvatarHash(UUID ownerId, String hash) {
        if (!RelayState.isRelayActive()) return;
        sender.sendAvatarHash(ownerId, hash);
    }

    public static void requestAvatar(UUID targetId) {
        if (!RelayState.isRelayActive()) return;
        sender.requestAvatar(targetId);
    }

    public static void sendAvatarData(UUID ownerId, byte[] data) {
        if (!RelayState.isRelayActive()) return;
        sender.sendAvatarData(ownerId, data);
    }

    public static void sendPing(UUID ownerId, int pingId, byte[] data, boolean sync) {
        if (!RelayState.isRelayActive()) return;
        sender.sendPing(ownerId, pingId, data, sync);
    }

    public static void sendHello() {
        sender.sendHello();
    }
}
