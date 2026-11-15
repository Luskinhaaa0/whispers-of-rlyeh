package com.whispersofrlyeh.network;

import com.whispersofrlyeh.sanity.ClientSanityData;
import com.whispersofrlyeh.space.ClientSpaceState;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(
                SanitySyncPayload.TYPE,
                SanitySyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientSanityData.set(payload.sanity())));

        registrar.playToClient(
                SpaceTransitionPayload.TYPE,
                SpaceTransitionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> ClientSpaceState.skipLoadingScreenUntil = System.currentTimeMillis() + 4000));
    }

    private ModNetworking() {}
}
