package org.figurarelay.mixin;

import org.figuramc.figura.gui.widgets.StatusWidget;
import org.figurarelay.client.RelayState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shows the relay state in the backend status slot instead of the
 * WebSocket backend status: green (3) when the relay handshake completed,
 * red (1) otherwise. Same as the previous fork's StatusWidget patch.
 */
@Mixin(value = StatusWidget.class, remap = false)
public class StatusWidgetMixin {

    @Shadow(remap = false)
    protected int status;

    @Inject(method = "tick()V", at = @At("TAIL"), require = 0)
    private void FiguraRelay$relayStatus(CallbackInfo ci) {
        int relay = RelayState.isRelayActive() ? 3 : 1;
        status = (status & ~(3 << 6)) | ((relay << 6));
    }
}
