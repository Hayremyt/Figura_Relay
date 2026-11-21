package org.figurarelay.client;

import net.minecraft.nbt.NbtIo;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.UserData;
import org.figuramc.figura.avatar.local.CacheAvatarLoader;
import org.figuramc.figura.backend2.NetworkStuff;
import org.figuramc.figura.utils.RefilledNumber;
import org.figurarelay.FiguraRelay;
import org.figurarelay.util.FiguraAccess;
import org.figurarelay.util.LastHashStore;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.UUID;

/**
 * relay replacements for Figura's backend API calls.
 * Invoked from NetworkStuffMixin. Client only.
 */
public class RelayUpload {

    static final RefilledNumber uploadRate = new RefilledNumber();
    static final RefilledNumber downloadRate = new RefilledNumber();

    public static void tickRates() {
        uploadRate.tick();
        downloadRate.tick();
    }

    public static boolean canUpload() {
        // Rate limiter is a dummy (no backend quotas in relay, same as the previous fork);
        // the relay gate lives in NetworkStuffMixin (relayActive && canUpload).
        return true;
    }

    /** Fetches user data. In relay mode badges default to empty; avatars arrive via packets. */
    public static void getUser(UserData user) {
        if (user.id.version() != 4) {
            FiguraMod.debug("Voiding request for non v4 UUID \"" + user.id + "\" (v" + user.id.version() + ")");
            return;
        }

        var badges = new BitSet[]{new BitSet(), new BitSet()};
        user.loadData(new ArrayList<>(), com.mojang.datafixers.util.Pair.of(badges[0], badges[1]));
    }

    /** Uploads avatar. Relay mode: full NBT to server. Offline: local cache only. */
    public static void uploadAvatar(Avatar avatar) {
        if (avatar == null || avatar.nbt == null)
            return;

        if (!RelayState.isRelayActive()) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                NbtIo.writeCompressed(avatar.nbt, baos);
                byte[] nbtBytes = baos.toByteArray();
                String hash = Integer.toHexString(java.util.Arrays.hashCode(nbtBytes));
                CacheAvatarLoader.save(hash, avatar.nbt);
                AvatarManager.localUploaded = true;
                uploadRate.use();
                baos.close();
            } catch (Exception e) {
                FiguraRelay.LOGGER.error("Relay: failed to cache avatar locally", e);
            }
            return;
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            NbtIo.writeCompressed(avatar.nbt, baos);
            byte[] nbtBytes = baos.toByteArray();

            if (nbtBytes.length > RelayLimits.maxAvatarSize) {
                FiguraRelay.LOGGER.warn("Relay: avatar is too large: {} > {}", nbtBytes.length, RelayLimits.maxAvatarSize);
                return;
            }

            String hash = Integer.toHexString(java.util.Arrays.hashCode(nbtBytes));
            CacheAvatarLoader.save(hash, avatar.nbt);
            AvatarManager.localUploaded = true;

            // Send full NBT to server — server saves it and broadcasts the hash
            UUID localId = FiguraMod.getLocalPlayerUUID();
            if (localId != null) {
                RelaySender.sendAvatarData(localId, nbtBytes);
            }

            uploadRate.use();
            baos.close();
        } catch (Exception e) {
            FiguraRelay.LOGGER.error("Relay: failed to upload avatar", e);
        }
    }

    /** Deletes avatar — clears local data and broadcasts an empty hash. */
    public static void deleteAvatar(String avatarId) {
        FiguraRelay.LOGGER.info("Relay: deleteAvatar, relayActive={}", RelayState.isRelayActive());
        CacheAvatarLoader.clearCache();

        if (RelayState.isRelayActive()) {
            UUID localId = FiguraMod.getLocalPlayerUUID();
            if (localId != null) {
                AvatarManager.clearAvatars(localId);
                FiguraAccess.fetchedUsers().remove(localId);
                LastHashStore.remove(localId);
                RelaySender.sendAvatarHash(localId, "");
            }
        }
    }

    /** Equips avatar — in relay mode just re-uploads the current one. */
    public static void equipAvatar(java.util.List<com.mojang.datafixers.util.Pair<UUID, String>> avatars) {
        uploadAvatar(AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID()));
    }

    /** Requests avatar data for a player via the relay (rate-limited). */
    public static void getAvatar(UserData target, UUID owner, String id, String hash) {
        if (target.id.version() != 4)
            return;
        if (!RelayState.isRelayActive())
            return;

        Avatar mainAvatar = AvatarManager.getAvatarForPlayer(owner);
        if (mainAvatar != null) {
            String lastHash = LastHashStore.get(owner);
            if (hash != null && hash.equals(lastHash)) {
                return;
            }
        }

        if (RelayState.requestAvatar(owner)) downloadRate.use();
    }

    /** Sends a Lua ping through the relay. */
    public static void sendPing(int id, boolean sync, byte[] data) {
        if (!AvatarManager.localUploaded)
            return;
        if (!RelayState.isRelayActive())
            return;

        try {
            UUID localId = FiguraMod.getLocalPlayerUUID();
            if (localId != null) {
                RelaySender.sendPing(localId, id, data, sync);
            }

            NetworkStuff.pingsSent++;
            if (NetworkStuff.lastPing == 0)
                NetworkStuff.lastPing = FiguraMod.ticks;
        } catch (Exception e) {
            FiguraRelay.LOGGER.error("Relay: failed to send ping", e);
        }
    }
}
