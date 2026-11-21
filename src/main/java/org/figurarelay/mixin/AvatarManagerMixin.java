package org.figurarelay.mixin;

import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.UserData;
import org.figuramc.figura.utils.EntityUtils;
import org.figuramc.figura.backend2.NetworkStuff;
import org.figurarelay.client.RelaySender;
import org.figurarelay.client.RelayState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Replaces Figura's backend avatar fetching with the relay flow.
 * Applies on the client only.
 */
@Mixin(value = AvatarManager.class, remap = false)
public class AvatarManagerMixin {

    @Shadow(remap = false)
    private static Map<UUID, UserData> LOADED_USERS;

    @Shadow(remap = false)
    private static Set<UUID> FETCHED_USERS;

    @Inject(method = "fetchBackend(Ljava/util/UUID;)V", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$fetchBackend(UUID id, CallbackInfo ci) {
        if (FETCHED_USERS.contains(id)) {
            ci.cancel();
            return;
        }

        FETCHED_USERS.add(id);

        if (EntityUtils.checkInvalidPlayer(id)) {
            ci.cancel();
            return;
        }

        UserData user = LOADED_USERS.computeIfAbsent(id, UserData::new);
        // relay getUser: empty badges; avatar data arrives via hash broadcasts
        NetworkStuff.getUser(user);
        ci.cancel();
    }

    @Inject(method = "reloadAvatar(Ljava/util/UUID;)V", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$reloadAvatar(UUID id, CallbackInfo ci) {
        if (!RelayState.isRelayActive())
            return; // fall through to the original behavior

        // Same as the previous fork: unsaved local disk edits always reload
        // from disk, so the reload key never discards them for a server copy.
        if (!AvatarManager.localUploaded && org.figuramc.figura.FiguraMod.isLocal(id))
            return; // fall through to loadLocalAvatar(lastPath)

        // Otherwise drop the local copy and fetch the server copy
        AvatarManager.clearAvatars(id);
        FETCHED_USERS.remove(id);
        RelaySender.requestAvatar(id);
        ci.cancel();
    }
}
