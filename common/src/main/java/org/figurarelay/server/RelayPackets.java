package org.figurarelay.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerPlayer;
import org.figurarelay.net.DataPayload;
import org.figurarelay.net.HashPayload;
import org.figurarelay.net.HelloPayload;
import org.figurarelay.net.PingPayload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Server-side packet logic. Transport-agnostic: the platform modules
 * supply the actual send functions. Figura is NOT required here.
 */
public class RelayPackets {

    private static final Set<UUID> playersWithMod = ConcurrentHashMap.newKeySet();

    public static boolean hasMod(ServerPlayer player) {
        return playersWithMod.contains(player.getUUID());
    }

    public static void removePlayer(UUID playerId) {
        playersWithMod.remove(playerId);
    }

    public static void unbanPlayer(UUID playerId) {
        BanStore.unban(playerId);
    }

    public static void handleHello(ServerPlayer player,
                                   BiConsumer<ServerPlayer, HelloPayload> sendHelloBack,
                                   BiConsumer<ServerPlayer, HashPayload> sendHash,
                                   BiConsumer<ServerPlayer, HashPayload> notifyOthers) {
        UUID playerId = player.getUUID();

        if (BanStore.isBanned(playerId)) {
            if (RelayConfig.isLogOperations())
                RelayMain.LOGGER.debug("Relay: banned player {} tried to use relay", playerId);
            return;
        }

        playersWithMod.add(playerId);
        if (RelayConfig.isLogOperations())
            RelayMain.LOGGER.debug("Relay: player {} has the companion mod ({} total)", playerId, playersWithMod.size());

        sendHelloBack.accept(player, new HelloPayload(RelayConfig.getMaxAvatarSize()));

        AvatarStore.getAllHashes().forEach((ownerId, hash) -> {
            if (hash != null && !hash.isEmpty()) {
                sendHash.accept(player, new HashPayload(ownerId, hash));
            }
        });

        String myHash = AvatarStore.getPlayerHash(playerId);
        if (myHash != null && !myHash.isEmpty()) {
            HashPayload hashPayload = new HashPayload(playerId, myHash);
            for (ServerPlayer other : player.getServer().getPlayerList().getPlayers()) {
                if (other.getUUID().equals(playerId)) continue;
                if (!hasMod(other)) continue;
                notifyOthers.accept(other, hashPayload);
            }
        }
    }

    public static void handleHash(ServerPlayer sender, UUID ownerId, String hash,
                                   BiConsumer<ServerPlayer, HashPayload> relay) {
        if (!hasMod(sender)) return;
        if (!ownerId.equals(sender.getUUID())) return;
        if (hash == null || hash.isEmpty()) {
            AvatarStore.remove(ownerId);
        } else {
            AvatarStore.save(ownerId, hash, null);
        }
        HashPayload payload = new HashPayload(ownerId, hash != null ? hash : "");
        for (ServerPlayer p : sender.getServer().getPlayerList().getPlayers()) {
            if (p.getUUID().equals(sender.getUUID())) continue;
            if (!hasMod(p)) continue;
            relay.accept(p, payload);
        }
    }

    public static void handleRequest(ServerPlayer requester, UUID targetId,
                                      BiConsumer<ServerPlayer, DataPayload> sendData) {
        if (!hasMod(requester)) return;
        CompoundTag nbt = AvatarStore.load(targetId);
        if (nbt == null) return;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            NbtIo.writeCompressed(nbt, baos);
            byte[] bytes = baos.toByteArray();
            baos.close();
            sendData.accept(requester, new DataPayload(targetId, bytes));
        } catch (Exception e) {
            RelayMain.LOGGER.error("Relay: failed to send avatar for " + targetId, e);
        }
    }

    public static void handleData(ServerPlayer sender, UUID ownerId, byte[] data,
                                   BiConsumer<ServerPlayer, HashPayload> relay) {
        if (!hasMod(sender)) return;
        if (!ownerId.equals(sender.getUUID())) {
            if (RelayConfig.isLogOperations())
                RelayMain.LOGGER.debug("Relay: player {} tried to upload avatar for {}", sender.getUUID(), ownerId);
            return;
        }
        if (data.length > RelayConfig.getMaxAvatarSize()) return;
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            CompoundTag nbt = NbtIo.readCompressed(bais, NbtAccounter.unlimitedHeap());
            bais.close();
            String hash = Integer.toHexString(java.util.Arrays.hashCode(data));
            AvatarStore.save(ownerId, hash, nbt);
            HashPayload payload = new HashPayload(ownerId, hash);
            for (ServerPlayer p : sender.getServer().getPlayerList().getPlayers()) {
                if (p.getUUID().equals(sender.getUUID())) continue;
                if (!hasMod(p)) continue;
                relay.accept(p, payload);
            }
        } catch (Exception e) {
            RelayMain.LOGGER.error("Relay: failed to deserialize avatar from " + ownerId, e);
        }
    }

    public static void handlePing(ServerPlayer sender, UUID ownerId, int pingId, byte[] data, boolean sync,
                                   BiConsumer<ServerPlayer, PingPayload> relay) {
        if (!hasMod(sender)) return;
        if (data.length > RelayConfig.getMaxAvatarSize()) return;
        PingPayload payload = new PingPayload(ownerId, pingId, data, sync);
        for (ServerPlayer p : sender.getServer().getPlayerList().getPlayers()) {
            if (!hasMod(p)) continue;
            // sync=true: sender did NOT run locally, so relay must include them.
            // sync=false: sender already ran locally, skip to avoid double execution.
            if (!sync && p.getUUID().equals(sender.getUUID())) continue;
            relay.accept(p, payload);
        }
    }

    public static void banPlayer(ServerPlayer bannedPlayer, net.minecraft.server.MinecraftServer server,
                                  java.util.function.Consumer<ServerPlayer> notifyOthers,
                                  java.util.function.Consumer<ServerPlayer> sendRelayOff) {
        UUID playerId = bannedPlayer.getUUID();
        BanStore.ban(playerId);
        playersWithMod.remove(playerId);
        AvatarStore.remove(playerId);
        sendRelayOff.accept(bannedPlayer);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.getUUID().equals(playerId)) continue;
            if (!hasMod(p)) continue;
            notifyOthers.accept(p);
        }
    }

    public static void banPlayer(UUID playerId) {
        BanStore.ban(playerId);
        playersWithMod.remove(playerId);
        AvatarStore.remove(playerId);
    }
}
