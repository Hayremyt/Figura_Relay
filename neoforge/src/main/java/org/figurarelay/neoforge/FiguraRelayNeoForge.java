package org.figurarelay.neoforge;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
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
 * Combined client + server entry. Client wiring only runs on Dist.CLIENT;
 * the relay runs everywhere (dedicated + integrated servers).
 */
@Mod("figura_relay")
public class FiguraRelayNeoForge {

    public FiguraRelayNeoForge(IEventBus modBus, ModContainer container) {
        FiguraRelay.LOGGER.info("Figura Relay (NeoForge) initializing...");

        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            PayloadRegistrar registrar = event.registrar("figura_relay").optional();

            // Order MUST match any old installs:
            // 0: Hello, 1: Hash, 2: Request, 3: Data, 4: Ping, 5: RelayOff
            registrar.playBidirectional(HelloPayload.TYPE, HelloPayload.CODEC, (payload, context) -> {
                if (context.flow().isClientbound()) invokeClient("handleHello", new Class[]{int.class}, payload.maxAvatarSize());
                else enqueueServer(context, () -> {
                    ServerPlayer player = (ServerPlayer) context.player();
                    RelayPackets.handleHello(player, FiguraRelayNeoForge::sendTo, FiguraRelayNeoForge::sendTo, FiguraRelayNeoForge::sendTo);
                });
            });
            registrar.playBidirectional(HashPayload.TYPE, HashPayload.CODEC, (payload, context) -> {
                if (context.flow().isClientbound()) invokeClient("handleHash", new Class[]{java.util.UUID.class, String.class}, payload.ownerId(), payload.hash());
                else enqueueServer(context, () -> {
                    ServerPlayer sender = (ServerPlayer) context.player();
                    RelayPackets.handleHash(sender, payload.ownerId(), payload.hash(), FiguraRelayNeoForge::sendTo);
                });
            });
            registrar.playBidirectional(RequestPayload.TYPE, RequestPayload.CODEC, (payload, context) -> {
                if (!context.flow().isClientbound()) enqueueServer(context, () -> {
                    ServerPlayer requester = (ServerPlayer) context.player();
                    RelayPackets.handleRequest(requester, payload.targetId(), FiguraRelayNeoForge::sendTo);
                });
            });
            registrar.playBidirectional(DataPayload.TYPE, DataPayload.CODEC, (payload, context) -> {
                if (context.flow().isClientbound()) invokeClient("handleData", new Class[]{java.util.UUID.class, byte[].class}, payload.ownerId(), payload.data());
                else enqueueServer(context, () -> {
                    ServerPlayer sender = (ServerPlayer) context.player();
                    RelayPackets.handleData(sender, payload.ownerId(), payload.data(), FiguraRelayNeoForge::sendTo);
                });
            });
            registrar.playBidirectional(PingPayload.TYPE, PingPayload.CODEC, (payload, context) -> {
                if (context.flow().isClientbound())
                    invokeClient("handlePing", new Class[]{java.util.UUID.class, int.class, byte[].class, boolean.class}, payload.ownerId(), payload.pingId(), payload.data(), payload.sync());
                else enqueueServer(context, () -> {
                    ServerPlayer sender = (ServerPlayer) context.player();
                    RelayPackets.handlePing(sender, payload.ownerId(), payload.pingId(), payload.data(), payload.sync(), FiguraRelayNeoForge::sendTo);
                });
            });
            registrar.playBidirectional(RelayOffPayload.TYPE, RelayOffPayload.CODEC, (payload, context) -> {
                if (context.flow().isClientbound()) invokeClient("handleRelayOff", new Class[]{});
            });
        });

        NeoForge.EVENT_BUS.addListener(this::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event ->
                RelayCommands.register(event.getDispatcher(), FiguraRelayNeoForge::sendTo));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                RelayPackets.removePlayer(player.getUUID());
            }
        });

        if (FMLEnvironment.dist == Dist.CLIENT
                && net.neoforged.fml.ModList.get().isLoaded("figura")) {
            try {
                Class.forName("org.figurarelay.neoforge.NeoForgeClientHelper").getMethod("initClient").invoke(null);
                FiguraRelay.LOGGER.info("Figura Relay (NeoForge) client patching enabled.");
            } catch (Throwable t) {
                FiguraRelay.LOGGER.info("Figura Relay (NeoForge) client not available: {}", t.toString());
            }
        }
    }

    private static void enqueueServer(IPayloadContext context, Runnable work) {
        context.enqueueWork(work);
    }

    private static void sendTo(ServerPlayer player, CustomPacketPayload payload) {
        player.connection.send(payload);
    }

    private void onServerAboutToStart(ServerAboutToStartEvent event) {
        RelayMain.init();
    }

    private static void invokeClient(String method, Class<?>[] paramTypes, Object... args) {
        try {
            Class.forName("org.figurarelay.neoforge.NeoForgeClientHelper").getMethod(method, paramTypes).invoke(null, args);
        } catch (Throwable ignored) {}
    }

}
