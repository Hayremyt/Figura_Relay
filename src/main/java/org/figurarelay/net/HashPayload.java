package org.figurarelay.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/** Avatar hash announcement (C2S upload, S2C broadcast). Empty hash = avatar removed. */
public record HashPayload(UUID ownerId, String hash) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("figura", "relay_hash");
    public static final Type<HashPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, HashPayload> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeUUID(p.ownerId); buf.writeUtf(p.hash != null ? p.hash : ""); },
            buf -> new HashPayload(buf.readUUID(), buf.readUtf())
    );
    @Override public @NotNull Type<?> type() { return TYPE; }
}
