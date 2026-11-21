package org.figurarelay.mixin;

import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.gui.screens.WardrobeScreen;
import org.figuramc.figura.gui.widgets.Button;
import org.figurarelay.client.RelaySender;
import org.figurarelay.client.RelayState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Makes the middle (reload) wardrobe button download the local player's
 * avatar from the relay server instead of just reloading it from disk.
 *
 * Targets the reload button's press handler. Verified against Figura 0.1.6
 * (lambda$init$1 in WardrobeScreen: clearAvatars + loadAvatar + auth);
 * if Figura changes, this mixin is skipped and the button keeps its
 * original local-reload behavior.
 */
@Mixin(value = WardrobeScreen.class, remap = false)
public class WardrobeScreenMixin {

    @Shadow(remap = false)
    private Button upload;

    @Shadow(remap = false)
    private Button delete;

    @Inject(method = "lambda$init$1(Lnet/minecraft/client/gui/components/Button;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private static void FiguraRelay$onReloadPressed(net.minecraft.client.gui.components.Button button, CallbackInfo ci) {
        if (!RelayState.isRelayActive())
            return; // fall through to the original local reload

        UUID localId = FiguraMod.getLocalPlayerUUID();
        AvatarManager.clearAvatars(localId);
        org.figurarelay.util.FiguraAccess.fetchedUsers().remove(localId);
        RelaySender.requestAvatar(localId);
        org.figuramc.figura.gui.widgets.lists.AvatarList.selectedEntry = null;
        ci.cancel();
    }

    @Inject(method = "tick()V", at = @At("TAIL"), require = 0)
    private void FiguraRelay$tickFix(CallbackInfo ci) {
        // Without relay there's nowhere to send — both buttons stay inactive.
        boolean relay = RelayState.isRelayActive()
                && net.minecraft.client.Minecraft.getInstance().getConnection() != null;
        // Same short-circuit as clean tick(): skip the fetch once uploaded.
        Avatar avatar = relay && !AvatarManager.localUploaded
                ? AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID()) : null;
        boolean errored = avatar != null && avatar.scriptError && !Configs.ALLOW_UPLOADING_ERRORED_AVATARS.value;
        upload.setActive(relay && !AvatarManager.localUploaded
                && avatar != null && avatar.nbt != null && avatar.loaded && !errored);
        upload.setTooltip(errored
                ? org.figuramc.figura.utils.FiguraText.of("gui.wardrobe.upload.errored", avatar.errorText).withStyle(net.minecraft.ChatFormatting.RED)
                : org.figuramc.figura.utils.FiguraText.of("gui.wardrobe.upload.tooltip"));
        delete.setActive(relay && AvatarManager.localUploaded);
    }
}
