package org.figurarelay.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/** Compressed avatar NBT (C2S upload, S2C delivery). */
public record DataPayload(UUID ownerId, byte[] data) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("figura", "relay_data");
    public static final Type<DataPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, DataPayload> CODEC = StreamCodec.of(
            (buf, p) -> { FriendlyByteBuf f = new FriendlyByteBuf(buf); f.writeUUID(p.ownerId); f.writeVarInt(p.data.length); f.writeBytes(p.data); },
            buf -> { FriendlyByteBuf f = new FriendlyByteBuf(buf); UUID oid = f.readUUID(); byte[] d = new byte[f.readVarInt()]; f.readBytes(d); return new DataPayload(oid, d); }
    );
    @Override public @NotNull Type<?> type() { return TYPE; }
}
