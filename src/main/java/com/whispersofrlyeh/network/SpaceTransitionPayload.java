package com.whispersofrlyeh.network;

import com.whispersofrlyeh.WhispersOfRlyeh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Avisa o cliente que vem aí uma troca de dimensão espacial (para pular a tela de carregamento). */
public record SpaceTransitionPayload() implements CustomPacketPayload {

    public static final Type<SpaceTransitionPayload> TYPE = new Type<>(WhispersOfRlyeh.id("space_transition"));

    public static final StreamCodec<ByteBuf, SpaceTransitionPayload> STREAM_CODEC =
            StreamCodec.unit(new SpaceTransitionPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
