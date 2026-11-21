package org.figurarelay.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.figurarelay.FiguraRelay;
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

/** Real client init — only loaded when Figura is present. */
public class FiguraRelayClientInit implements Runnable {

    @Override
    public void run() {
        safeRegister(() -> PayloadTypeRegistry.playC2S().register(HelloPayload.TYPE, HelloPayload.CODEC));
        safeRegister(() -> PayloadTypeRegistry.playS2C().register(HelloPayload.TYPE, HelloPayload.CODEC));
        safeRegister(() -> PayloadTypeRegistry.playC2S().register(HashPayload.TYPE, HashPayload.CODEC));
        safeRegister(() -> PayloadTypeRegistry.playS2C().register(HashPayload.TYPE, HashPayload.CODEC));
        safeRegister(() -> PayloadTypeRegistry.playC2S().register(RequestPayload.TYPE, RequestPayload.CODEC));
        safeRegister(() -> PayloadTypeRegistry.playC2S().register(DataPayload.TYPE, DataPayload.CODEC));
        safeRegister(() -> PayloadTypeRegistry.playS2C().register(DataPayload.TYPE, DataPayload.CODEC));
        safeRegister(() -> PayloadTypeRegistry.playC2S().register(PingPayload.TYPE, PingPayload.CODEC));
        safeRegister(() -> PayloadTypeRegistry.playS2C().register(PingPayload.TYPE, PingPayload.CODEC));
        safeRegister(() -> PayloadTypeRegistry.playS2C().register(RelayOffPayload.TYPE, RelayOffPayload.CODEC));

        ClientPlayNetworking.registerGlobalReceiver(HelloPayload.TYPE,
                (payload, context) -> RelayHandlers.handleServerHello(payload.maxAvatarSize()));
        ClientPlayNetworking.registerGlobalReceiver(HashPayload.TYPE,
                (payload, context) -> RelayHandlers.handleAvatarHash(payload.ownerId(), payload.hash()));
        ClientPlayNetworking.registerGlobalReceiver(DataPayload.TYPE,
                (payload, context) -> RelayHandlers.handleAvatarData(payload.ownerId(), payload.data()));
        ClientPlayNetworking.registerGlobalReceiver(PingPayload.TYPE,
                (payload, context) -> RelayHandlers.handlePing(payload.ownerId(), payload.pingId(), payload.data(), payload.sync()));
        ClientPlayNetworking.registerGlobalReceiver(RelayOffPayload.TYPE,
                (payload, context) -> RelayHandlers.handleRelayOff());

        RelaySender.init(new FabricSender());

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> RelayState.onClientJoinServer());

        FiguraRelay.LOGGER.info("Figura Relay (Fabric) client patching enabled.");
    }

    private static void safeRegister(Runnable r) {
        try { r.run(); } catch (IllegalArgumentException ignored) {}
    }

    private static class FabricSender implements RelaySender.Sender {
        @Override public void sendAvatarHash(UUID ownerId, String hash) {
            ClientPlayNetworking.send(new HashPayload(ownerId, hash));
        }
        @Override public void requestAvatar(UUID targetId) {
            ClientPlayNetworking.send(new RequestPayload(targetId));
        }
        @Override public void sendAvatarData(UUID ownerId, byte[] data) {
            ClientPlayNetworking.send(new DataPayload(ownerId, data));
        }
        @Override public void sendPing(UUID ownerId, int pingId, byte[] data, boolean sync) {
            ClientPlayNetworking.send(new PingPayload(ownerId, pingId, data, sync));
        }
        @Override public void sendHello() {
            ClientPlayNetworking.send(new HelloPayload());
        }
    }
}
