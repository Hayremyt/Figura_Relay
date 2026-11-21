package org.figurarelay.forge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
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

import java.util.UUID;

/**
 * Combined client + server entry.
 * Client wiring (Hello, sender) only runs on Dist.CLIENT and requires Figura;
 * the relay runs everywhere (dedicated + integrated servers).
 */
@Mod("figura_relay")
public class FiguraRelayForge {

    // C2S channel — client sends to server (same name as the old setup)
    private static final SimpleChannel C2S = ChannelBuilder
            .named(HelloPayload.ID)
            .networkProtocolVersion(1)
            .simpleChannel();

    // S2C channel — server sends to client (same name as the old setup)
    private static final SimpleChannel S2C = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath("figura", "relay_s2c"))
            .networkProtocolVersion(1)
            .simpleChannel();

    public FiguraRelayForge() {
        FiguraRelay.LOGGER.info("Figura Relay (Forge) initializing...");
        registerMessages();

        // -- server side (dedicated + integrated) -- //
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> RelayMain.init());
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                RelayCommands.register(event.getDispatcher(), (p, payload) ->
                        S2C.send(payload, PacketDistributor.PLAYER.with(p))));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                RelayPackets.removePlayer(player.getUUID());
            }
        });

        // -- client side (requires Figura) -- //
        if (FMLEnvironment.dist == Dist.CLIENT
                && net.minecraftforge.fml.ModList.get().isLoaded("figura")) {
            try {
                Class.forName("org.figurarelay.forge.ForgeClientHelper")
                        .getMethod("initClient", SimpleChannel.class)
                        .invoke(null, C2S);
                FiguraRelay.LOGGER.info("Figura Relay (Forge) client patching enabled.");
            } catch (Throwable t) {
                FiguraRelay.LOGGER.info("Figura Relay (Forge) client not available (Figura not installed): {}", t.toString());
            }
        }
    }

    private static void sendTo(ServerPlayer player, Object payload) {
        if (payload instanceof HelloPayload h) S2C.send(h, PacketDistributor.PLAYER.with(player));
        else if (payload instanceof HashPayload h) S2C.send(h, PacketDistributor.PLAYER.with(player));
        else if (payload instanceof DataPayload d) S2C.send(d, PacketDistributor.PLAYER.with(player));
        else if (payload instanceof PingPayload p) S2C.send(p, PacketDistributor.PLAYER.with(player));
        else if (payload instanceof RelayOffPayload r) S2C.send(r, PacketDistributor.PLAYER.with(player));
    }

    private static void registerMessages() {
        int c2sId = 0;

        // ── C2S (client → server) ──
        C2S.messageBuilder(HelloPayload.class, c2sId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder((p, buf) -> {})
                .decoder(buf -> new HelloPayload())
                .consumerMainThread((payload, ctx) -> {
                    ServerPlayer player = ctx.getSender();
                    if (player == null) return;
                    RelayPackets.handleHello(player, FiguraRelayForge::sendTo, FiguraRelayForge::sendTo, FiguraRelayForge::sendTo);
                })
                .add();

        C2S.messageBuilder(HashPayload.class, c2sId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder((p, buf) -> { buf.writeUUID(p.ownerId()); buf.writeUtf(p.hash() != null ? p.hash() : ""); })
                .decoder(buf -> new HashPayload(buf.readUUID(), buf.readUtf()))
                .consumerMainThread((payload, ctx) -> {
                    ServerPlayer sender = ctx.getSender();
                    if (sender == null) return;
                    RelayPackets.handleHash(sender, payload.ownerId(), payload.hash(), FiguraRelayForge::sendTo);
                })
                .add();

        C2S.messageBuilder(RequestPayload.class, c2sId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder((p, buf) -> buf.writeUUID(p.targetId()))
                .decoder(buf -> new RequestPayload(buf.readUUID()))
                .consumerMainThread((payload, ctx) -> {
                    ServerPlayer requester = ctx.getSender();
                    if (requester == null) return;
                    RelayPackets.handleRequest(requester, payload.targetId(), FiguraRelayForge::sendTo);
                })
                .add();

        C2S.messageBuilder(DataPayload.class, c2sId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder((p, buf) -> { buf.writeUUID(p.ownerId()); buf.writeVarInt(p.data().length); buf.writeBytes(p.data()); })
                .decoder(buf -> { UUID oid = buf.readUUID(); byte[] d = new byte[buf.readVarInt()]; buf.readBytes(d); return new DataPayload(oid, d); })
                .consumerMainThread((payload, ctx) -> {
                    ServerPlayer sender = ctx.getSender();
                    if (sender == null) return;
                    RelayPackets.handleData(sender, payload.ownerId(), payload.data(), FiguraRelayForge::sendTo);
                })
                .add();

        C2S.messageBuilder(PingPayload.class, c2sId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder((p, buf) -> { buf.writeUUID(p.ownerId()); buf.writeVarInt(p.pingId()); buf.writeBoolean(p.sync()); buf.writeVarInt(p.data().length); buf.writeBytes(p.data()); })
                .decoder(buf -> { UUID oid = buf.readUUID(); int pid = buf.readVarInt(); boolean s = buf.readBoolean(); byte[] d = new byte[buf.readVarInt()]; buf.readBytes(d); return new PingPayload(oid, pid, d, s); })
                .consumerMainThread((payload, ctx) -> {
                    ServerPlayer sender = ctx.getSender();
                    if (sender == null) return;
                    RelayPackets.handlePing(sender, payload.ownerId(), payload.pingId(), payload.data(), payload.sync(), FiguraRelayForge::sendTo);
                })
                .add();

        // ── S2C (server → client) ──
        // Index order MUST match the client registrations above and any old installs:
        // 0: Hello, 1: Hash, 2: Data, 3: Ping, 4: RelayOff
        int s2cId = 0;

        S2C.messageBuilder(HelloPayload.class, s2cId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((p, buf) -> buf.writeVarInt(p.maxAvatarSize()))
                .decoder(buf -> new HelloPayload(buf.readVarInt()))
                .consumerMainThread((payload, ctx) -> {
                    if (ctx.isClientSide()) invokeClient("handleHello", new Class[]{int.class}, payload.maxAvatarSize());
                })
                .add();

        S2C.messageBuilder(HashPayload.class, s2cId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((p, buf) -> { buf.writeUUID(p.ownerId()); buf.writeUtf(p.hash() != null ? p.hash() : ""); })
                .decoder(buf -> new HashPayload(buf.readUUID(), buf.readUtf()))
                .consumerMainThread((payload, ctx) -> {
                    if (ctx.isClientSide()) invokeClient("handleHash", new Class[]{UUID.class, String.class}, payload.ownerId(), payload.hash());
                })
                .add();

        S2C.messageBuilder(DataPayload.class, s2cId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((p, buf) -> { buf.writeUUID(p.ownerId()); buf.writeVarInt(p.data().length); buf.writeBytes(p.data()); })
                .decoder(buf -> { UUID oid = buf.readUUID(); byte[] d = new byte[buf.readVarInt()]; buf.readBytes(d); return new DataPayload(oid, d); })
                .consumerMainThread((payload, ctx) -> {
                    if (ctx.isClientSide()) invokeClient("handleData", new Class[]{UUID.class, byte[].class}, payload.ownerId(), payload.data());
                })
                .add();

        S2C.messageBuilder(PingPayload.class, s2cId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((p, buf) -> { buf.writeUUID(p.ownerId()); buf.writeVarInt(p.pingId()); buf.writeBoolean(p.sync()); buf.writeVarInt(p.data().length); buf.writeBytes(p.data()); })
                .decoder(buf -> { UUID oid = buf.readUUID(); int pid = buf.readVarInt(); boolean s = buf.readBoolean(); byte[] d = new byte[buf.readVarInt()]; buf.readBytes(d); return new PingPayload(oid, pid, d, s); })
                .consumerMainThread((payload, ctx) -> {
                    if (ctx.isClientSide()) invokeClient("handlePing", new Class[]{UUID.class, int.class, byte[].class, boolean.class}, payload.ownerId(), payload.pingId(), payload.data(), payload.sync());
                })
                .add();

        S2C.messageBuilder(RelayOffPayload.class, s2cId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((p, buf) -> {})
                .decoder(buf -> new RelayOffPayload())
                .consumerMainThread((payload, ctx) -> {
                    if (ctx.isClientSide()) invokeClient("handleRelayOff", new Class[]{}, new Object[]{});
                })
                .add();
    }

    private static void invokeClient(String method, Class<?>[] paramTypes, Object... args) {
        try {
            Class.forName("org.figurarelay.forge.ForgeClientHelper").getMethod(method, paramTypes).invoke(null, args);
        } catch (Throwable ignored) {}
    }
}
