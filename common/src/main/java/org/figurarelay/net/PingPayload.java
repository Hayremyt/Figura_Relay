package org.figurarelay.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/** Lua ping event (bidirectional). */
public record PingPayload(UUID ownerId, int pingId, byte[] data, boolean sync) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("figura", "relay_ping");
    public static final Type<PingPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, PingPayload> CODEC = StreamCodec.of(
            (buf, p) -> { FriendlyByteBuf f = new FriendlyByteBuf(buf); f.writeUUID(p.ownerId); f.writeVarInt(p.pingId); f.writeBoolean(p.sync); f.writeVarInt(p.data.length); f.writeBytes(p.data); },
            buf -> { FriendlyByteBuf f = new FriendlyByteBuf(buf); UUID oid = f.readUUID(); int pid = f.readVarInt(); boolean s = f.readBoolean(); byte[] d = new byte[f.readVarInt()]; f.readBytes(d); return new PingPayload(oid, pid, d, s); }
    );
    @Override public @NotNull Type<?> type() { return TYPE; }
}
