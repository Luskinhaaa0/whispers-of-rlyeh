package com.whispersofrlyeh.network;

import com.whispersofrlyeh.WhispersOfRlyeh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Pacote servidor -> cliente com o valor atual de sanidade. */
public record SanitySyncPayload(int sanity) implements CustomPacketPayload {

    public static final Type<SanitySyncPayload> TYPE = new Type<>(WhispersOfRlyeh.id("sanity_sync"));

    public static final StreamCodec<ByteBuf, SanitySyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SanitySyncPayload::sanity,
            SanitySyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
