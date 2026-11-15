package com.whispersofrlyeh.network;

import com.whispersofrlyeh.sanity.ClientSanityData;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(
                SanitySyncPayload.TYPE,
                SanitySyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientSanityData.set(payload.sanity())));
    }

    private ModNetworking() {}
}
