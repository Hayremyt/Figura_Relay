package org.figurarelay.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** Handshake: C2S empty, S2C carries the server max avatar size. */
public record HelloPayload(int maxAvatarSize) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("figura", "hello");
    public static final Type<HelloPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, HelloPayload> CODEC = StreamCodec.of(
            (buf, p) -> buf.writeVarInt(p.maxAvatarSize),
            buf -> new HelloPayload(buf.readVarInt())
    );

    public HelloPayload() {
        this(0);
    }

    @Override public @NotNull Type<?> type() { return TYPE; }
}
