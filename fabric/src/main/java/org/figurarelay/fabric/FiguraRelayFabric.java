package org.figurarelay.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.figurarelay.FiguraRelay;
import org.figurarelay.net.DataPayload;
import org.figurarelay.net.HashPayload;
import org.figurarelay.net.HelloPayload;
import org.figurarelay.net.PingPayload;
import org.figurarelay.net.RelayOffPayload;
import org.figurarelay.net.RequestPayload;
import org.figurarelay.server.RelayCommands;
import org.figurarelay.server.RelayMain;
import org.figurarelay.server.RelayPackets;

/**
 * Server side (runs on dedicated + integrated servers, and harmlessly on clients).
 * No Figura classes are referenced here, so no Figura is required.
 */
public class FiguraRelayFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        FiguraRelay.LOGGER.info("Figura Relay (Fabric) initializing...");

        // Payload types must be registered before handlers (idempotent for integrated server)
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

        ServerPlayNetworking.registerGlobalReceiver(HelloPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            RelayPackets.handleHello(
                    player,
                    (p, h) -> ServerPlayNetworking.send(p, h),
                    (p, h) -> ServerPlayNetworking.send(p, h),
                    (p, h) -> ServerPlayNetworking.send(p, h));
        });

        ServerPlayNetworking.registerGlobalReceiver(HashPayload.TYPE, (payload, context) -> {
            ServerPlayer sender = context.player();
            RelayPackets.handleHash(sender, payload.ownerId(), payload.hash(),
                    (p, h) -> ServerPlayNetworking.send(p, h));
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestPayload.TYPE, (payload, context) -> {
            ServerPlayer requester = context.player();
            RelayPackets.handleRequest(requester, payload.targetId(),
                    (p, d) -> ServerPlayNetworking.send(p, d));
        });

        ServerPlayNetworking.registerGlobalReceiver(DataPayload.TYPE, (payload, context) -> {
            ServerPlayer sender = context.player();
            RelayPackets.handleData(sender, payload.ownerId(), payload.data(),
                    (p, h) -> ServerPlayNetworking.send(p, h));
        });

        ServerPlayNetworking.registerGlobalReceiver(PingPayload.TYPE, (payload, context) -> {
            ServerPlayer sender = context.player();
            RelayPackets.handlePing(sender, payload.ownerId(), payload.pingId(), payload.data(), payload.sync(),
                    (p, ping) -> ServerPlayNetworking.send(p, ping));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                RelayPackets.removePlayer(handler.getPlayer().getUUID()));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> RelayMain.init());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                RelayCommands.register(dispatcher, (p, payload) -> ServerPlayNetworking.send(p, payload)));

        FiguraRelay.LOGGER.info("Figura Relay (Fabric) initialized.");
    }

    private static void safeRegister(Runnable r) {
        try { r.run(); } catch (IllegalArgumentException ignored) {}
    }
}
