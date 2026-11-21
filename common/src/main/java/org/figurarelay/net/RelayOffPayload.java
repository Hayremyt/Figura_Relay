package org.figurarelay.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** Server → client: relay disabled for this player (banned). S2C only. */
public record RelayOffPayload() implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("figura", "relay_off");
    public static final Type<RelayOffPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, RelayOffPayload> CODEC = StreamCodec.of(
            (buf, p) -> {},
            buf -> new RelayOffPayload()
    );
    @Override public @NotNull Type<?> type() { return TYPE; }
}
