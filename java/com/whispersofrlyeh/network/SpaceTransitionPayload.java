package com.whispersofrlyeh.network;

import com.whispersofrlyeh.WhispersOfRlyeh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Avisa o cliente que vem aí uma viagem espacial, e de que tipo (para a animação certa). */
public record SpaceTransitionPayload(int transitionType) implements CustomPacketPayload {

    public static final int LEAVE_EARTH = 0;
    public static final int ENTER_EARTH = 1;
    public static final int ENTER_PLANET = 2;
    public static final int LEAVE_PLANET = 3;

    public static final Type<SpaceTransitionPayload> TYPE = new Type<>(WhispersOfRlyeh.id("space_transition"));

    public static final StreamCodec<ByteBuf, SpaceTransitionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SpaceTransitionPayload::transitionType,
            SpaceTransitionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
