package org.figurarelay.mixin;

import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.UserData;
import org.figuramc.figura.backend2.NetworkStuff;
import org.figurarelay.client.RelayLimits;
import org.figurarelay.client.RelayState;
import org.figurarelay.client.RelayTick;
import org.figurarelay.client.RelayUpload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Replaces Figura's WebSocket backend with relay traffic.
 * Applies on the client only (see figura-relay.mixins.json "client" section);
 * the config is not required, so servers without Figura skip it silently.
 */
@Mixin(value = NetworkStuff.class, remap = false)
public class NetworkStuffMixin {

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void FiguraRelay$initBackendStatus(CallbackInfo ci) {
        // The relay is always "active" (same as the previous fork setup)
        NetworkStuff.backendStatus = 0;
    }

    // -- tick: run relay maintenance instead of the backend tick -- //

    @Inject(method = "tick()V", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$tick(CallbackInfo ci) {
        RelayTick.tick();
        ci.cancel();
    }

    // -- auth / connection: no backend to talk to -- //

    @Inject(method = "auth()V", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$auth(CallbackInfo ci) {
        NetworkStuff.disconnectedReason = null;
        ci.cancel();
    }

    @Inject(method = "reAuth()V", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$reAuth(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "authSuccess(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$authSuccess(String token, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "authFail(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$authFail(String reason, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "connect(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$connect(String token, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "disconnect(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$disconnect(String reason, CallbackInfo ci) {
        ci.cancel();
    }

    // -- api: version/limits/motd come from nowhere in relay mode -- //

    @Inject(method = "checkVersion()V", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$checkVersion(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "setLimits()V", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$setLimits(CallbackInfo ci) {
        RelayLimits.setLimits();
        ci.cancel();
    }

    @Inject(method = "clear(Ljava/util/UUID;)V", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$clear(UUID id, CallbackInfo ci) {
        RelayState.clearRequest(id);
        ci.cancel();
    }

    // -- api: avatar data flows through relay packets -- //

    @Inject(method = "getUser(Lorg/figuramc/figura/avatar/UserData;)V", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$getUser(UserData user, CallbackInfo ci) {
        RelayUpload.getUser(user);
        ci.cancel();
    }

    @Inject(method = "uploadAvatar(Lorg/figuramc/figura/avatar/Avatar;)V", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$uploadAvatar(Avatar avatar, CallbackInfo ci) {
        RelayUpload.uploadAvatar(avatar);
        ci.cancel();
    }

    @Inject(method = "deleteAvatar(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$deleteAvatar(String avatar, CallbackInfo ci) {
        RelayUpload.deleteAvatar(avatar);
        ci.cancel();
    }

    @Inject(method = "equipAvatar(Ljava/util/List;)V", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$equipAvatar(List<?> avatars, CallbackInfo ci) {
        RelayUpload.equipAvatar(null);
        ci.cancel();
    }

    @Inject(method = "getAvatar(Lorg/figuramc/figura/avatar/UserData;Ljava/util/UUID;Ljava/lang/String;Ljava/lang/String;)V",
            at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$getAvatar(UserData target, UUID owner, String id, String hash, CallbackInfo ci) {
        RelayUpload.getAvatar(target, owner, id, hash);
        ci.cancel();
    }

    // -- badges: no backend, no-op -- //

    @Inject(method = "setBadge(I)V", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$setBadge(int badgeId, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "clearBadge()V", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$clearBadge(CallbackInfo ci) {
        ci.cancel();
    }

    // -- ws: pings and subscriptions go through relay packets -- //

    @Inject(method = "sendPing(IZ[B)V", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$sendPing(int id, boolean sync, byte[] data, CallbackInfo ci) {
        RelayUpload.sendPing(id, sync, data);
        ci.cancel();
    }

    @Inject(method = "subscribeAll()V", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$subscribeAll(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "unsubscribeAll()V", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$unsubscribeAll(CallbackInfo ci) {
        ci.cancel();
    }

    // -- resources: bundled with the mod -- //

    @Inject(method = "getResourcesHashes(Ljava/lang/String;)Ljava/io/InputStream;", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$getResourcesHashes(String version, CallbackInfoReturnable<InputStream> cir) {
        cir.setReturnValue(null);
    }

    @Inject(method = "getResource(Ljava/lang/String;Ljava/lang/String;)Ljava/io/InputStream;", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$getResource(String version, String resource, CallbackInfoReturnable<InputStream> cir) {
        cir.setReturnValue(null);
    }

    // -- status: relay is the transport — without it there's nowhere to send -- //

    @Inject(method = "isConnected()Z", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$isConnected(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(org.figurarelay.client.RelayState.isRelayActive()
                && net.minecraft.client.Minecraft.getInstance().getConnection() != null);
    }

    @Inject(method = "canUpload()Z", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$canUpload(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(org.figurarelay.client.RelayState.isRelayActive()
                && org.figurarelay.client.RelayUpload.canUpload());
    }

    @Inject(method = "getSizeLimit()I", at = @At("HEAD"), cancellable = true)
    private static void FiguraRelay$getSizeLimit(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(RelayLimits.maxAvatarSize);
    }
}
