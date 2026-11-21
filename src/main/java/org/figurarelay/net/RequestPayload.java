package org.figurarelay.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/** Request avatar NBT for a player (C2S only). */
public record RequestPayload(UUID targetId) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("figura", "relay_req");
    public static final Type<RequestPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestPayload> CODEC = StreamCodec.of(
            (buf, p) -> buf.writeUUID(p.targetId),
            buf -> new RequestPayload(buf.readUUID())
    );
    @Override public @NotNull Type<?> type() { return TYPE; }
}
