package org.figurarelay.forge;

import net.minecraftforge.network.SimpleChannel;
import org.figurarelay.client.RelayHandlers;
import org.figurarelay.client.RelaySender;
import org.figurarelay.client.RelayState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;

/** Client-only helpers — only loaded when Figura is present on the client. */
public class ForgeClientHelper {

    public static void handleHello(int maxAvatarSize) {
        RelayHandlers.handleServerHello(maxAvatarSize);
    }

    public static void handleHash(java.util.UUID ownerId, String hash) {
        RelayHandlers.handleAvatarHash(ownerId, hash);
    }

    public static void handleData(java.util.UUID ownerId, byte[] data) {
        RelayHandlers.handleAvatarData(ownerId, data);
    }

    public static void handlePing(java.util.UUID ownerId, int pingId, byte[] data, boolean sync) {
        RelayHandlers.handlePing(ownerId, pingId, data, sync);
    }

    public static void handleRelayOff() {
        RelayHandlers.handleRelayOff();
    }

    public static void initClient(SimpleChannel c2s) {
        RelaySender.init(new ForgeSender(c2s));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) ->
                RelayState.onClientJoinServer());
    }

    private static class ForgeSender implements RelaySender.Sender {
        private final SimpleChannel c2s;
        ForgeSender(SimpleChannel c2s) { this.c2s = c2s; }
        @Override public void sendAvatarHash(java.util.UUID ownerId, String hash) {
            c2s.send(new org.figurarelay.net.HashPayload(ownerId, hash), net.minecraftforge.network.PacketDistributor.SERVER.noArg());
        }
        @Override public void requestAvatar(java.util.UUID targetId) {
            c2s.send(new org.figurarelay.net.RequestPayload(targetId), net.minecraftforge.network.PacketDistributor.SERVER.noArg());
        }
        @Override public void sendAvatarData(java.util.UUID ownerId, byte[] data) {
            c2s.send(new org.figurarelay.net.DataPayload(ownerId, data), net.minecraftforge.network.PacketDistributor.SERVER.noArg());
        }
        @Override public void sendPing(java.util.UUID ownerId, int pingId, byte[] data, boolean sync) {
            c2s.send(new org.figurarelay.net.PingPayload(ownerId, pingId, data, sync), net.minecraftforge.network.PacketDistributor.SERVER.noArg());
        }
        @Override public void sendHello() {
            c2s.send(new org.figurarelay.net.HelloPayload(), net.minecraftforge.network.PacketDistributor.SERVER.noArg());
        }
    }
}
