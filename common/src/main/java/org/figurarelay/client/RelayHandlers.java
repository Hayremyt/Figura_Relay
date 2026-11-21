package org.figurarelay.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.UserData;
import org.figurarelay.FiguraRelay;
import org.figurarelay.util.FiguraAccess;
import org.figurarelay.util.LastHashStore;

import java.io.ByteArrayInputStream;
import java.util.UUID;

/**
 * Incoming relay packets. Client only (drives Figura objects).
 * Called from the platform receivers on the client thread.
 */
public class RelayHandlers {

    public static void handleAvatarHash(UUID ownerId, String hash) {
        if (!RelayState.isRelayActive()) return;

        if (hash == null || hash.isEmpty()) {
            AvatarManager.clearAvatars(ownerId);
            LastHashStore.remove(ownerId);
            return;
        }

        UserData userData = FiguraAccess.loadedUsers().computeIfAbsent(ownerId, UserData::new);

        String lastHash = LastHashStore.get(ownerId);
        if (!hash.equals(lastHash) || userData.getMainAvatar() == null) {
            RelaySender.requestAvatar(ownerId);
        }
    }

    public static void handleAvatarData(UUID ownerId, byte[] nbtBytes) {
        if (!RelayState.isRelayActive()) return;

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(nbtBytes);
            CompoundTag nbt = NbtIo.readCompressed(bais, NbtAccounter.unlimitedHeap());
            bais.close();

            String hash = Integer.toHexString(java.util.Arrays.hashCode(nbtBytes));
            FiguraAccess.loadedUsers().computeIfAbsent(ownerId, UserData::new);
            LastHashStore.set(ownerId, hash);
            // Same as clean backend getAvatar: seed the local cache so the
            // cacheCheck fallback can hit for others' avatars too.
            org.figuramc.figura.avatar.local.CacheAvatarLoader.save(hash, nbt);

            AvatarManager.setAvatar(ownerId, nbt);
            FiguraAccess.fetchedUsers().add(ownerId);
        } catch (Exception e) {
            FiguraRelay.LOGGER.error("Relay: failed to load avatar for " + ownerId, e);
        }
    }

    public static void handlePing(UUID ownerId, int pingId, byte[] data, boolean sync) {
        if (!RelayState.isRelayActive()) return;

        Avatar avatar = AvatarManager.getAvatarForPlayer(ownerId);
        if (avatar == null || avatar.luaRuntime == null || avatar.scriptError || !avatar.loaded) return;
        if (avatar.tick == null) return;

        avatar.runPing(pingId, data);

        org.figuramc.figura.backend2.NetworkStuff.pingsReceived++;
        if (org.figuramc.figura.backend2.NetworkStuff.lastPing == 0)
            org.figuramc.figura.backend2.NetworkStuff.lastPing = FiguraMod.ticks;
    }

    public static void handleRelayOff() {
        RelayState.onRelayOff();
        FiguraMod.LOGGER.info("Relay: relay disabled by server");
    }

    public static void handleServerHello(int maxAvatarSize) {
        RelayState.onServerHello(maxAvatarSize);
    }
}
