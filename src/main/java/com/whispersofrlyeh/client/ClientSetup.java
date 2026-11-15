package com.whispersofrlyeh.client;

import com.whispersofrlyeh.WhispersOfRlyeh;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** Coisas que só existem no cliente (tela, HUD). */
@EventBusSubscriber(modid = WhispersOfRlyeh.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        // Vinheta embaixo da HUD (sobre o mundo); barra por cima de tudo
        event.registerBelowAll(WhispersOfRlyeh.id("madness_vignette"), SanityHud::renderVignette);
        event.registerAboveAll(WhispersOfRlyeh.id("sanity_bar"), SanityHud::renderBar);
    }

    private ClientSetup() {}
}
