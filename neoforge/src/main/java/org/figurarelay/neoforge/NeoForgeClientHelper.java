package org.figurarelay.neoforge;

import io.netty.channel.Channel;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.figurarelay.client.RelayHandlers;
import org.figurarelay.client.RelaySender;
import org.figurarelay.client.RelayState;
import org.figurarelay.net.DataPayload;
import org.figurarelay.net.HashPayload;
import org.figurarelay.net.HelloPayload;
import org.figurarelay.net.PingPayload;
import org.figurarelay.net.RelayOffPayload;
import org.figurarelay.net.RequestPayload;

import java.util.UUID;

/** Client-only helpers — only loaded when Figura is present on the client. */
public class NeoForgeClientHelper {

    public static void handleHello(int maxAvatarSize) {
        RelayHandlers.handleServerHello(maxAvatarSize);
    }

    public static void handleHash(UUID ownerId, String hash) {
        RelayHandlers.handleAvatarHash(ownerId, hash);
    }

    public static void handleData(UUID ownerId, byte[] data) {
        RelayHandlers.handleAvatarData(ownerId, data);
    }

    public static void handlePing(UUID ownerId, int pingId, byte[] data, boolean sync) {
        RelayHandlers.handlePing(ownerId, pingId, data, sync);
    }

    public static void handleRelayOff() {
        RelayHandlers.handleRelayOff();
    }

    public static void initClient() {
        RelaySender.init(new NeoForgeSender());
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingIn.class,
                loginEvent -> RelayState.onClientJoinServer());
    }

    private static class NeoForgeSender implements RelaySender.Sender {
        @Override public void sendAvatarHash(UUID ownerId, String hash) {
            send(new HashPayload(ownerId, hash));
        }
        @Override public void requestAvatar(UUID targetId) {
            send(new RequestPayload(targetId));
        }
        @Override public void sendAvatarData(UUID ownerId, byte[] data) {
            send(new DataPayload(ownerId, data));
        }
        @Override public void sendPing(UUID ownerId, int pingId, byte[] data, boolean sync) {
            send(new PingPayload(ownerId, pingId, data, sync));
        }
        @Override public void sendHello() {
            send(new HelloPayload());
        }
        private void send(CustomPacketPayload payload) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null || minecraft.getConnection() == null || minecraft.player == null) return;
            try {
                Channel channel = minecraft.getConnection().getConnection().channel();
                if (channel != null && channel.isActive()) {
                    channel.writeAndFlush(new ServerboundCustomPayloadPacket(payload));
                }
            } catch (Exception ignored) {}
        }
    }
}
