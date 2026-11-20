package com.whispersofrlyeh.client;

import com.whispersofrlyeh.WhispersOfRlyeh;
import com.whispersofrlyeh.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** Coisas que só existem no cliente (tela, HUD, renderizadores). */
@EventBusSubscriber(modid = WhispersOfRlyeh.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        // Vinheta embaixo da HUD (sobre o mundo); barra por cima de tudo
        event.registerBelowAll(WhispersOfRlyeh.id("madness_vignette"), SanityHud::renderVignette);
        event.registerAboveAll(WhispersOfRlyeh.id("sanity_bar"), SanityHud::renderBar);
        // Céu escurecendo na subida + animações de viagem espacial
        event.registerBelowAll(WhispersOfRlyeh.id("space_transition"), SpaceTransitionOverlay::render);
    }

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        // "effects": "whispersofrlyeh:space" no dimension_type/space.json
        event.register(WhispersOfRlyeh.id("space"), new SpaceSkyEffects(SpaceSkyEffects.Mode.SPACE));
        event.register(WhispersOfRlyeh.id("moon"), new SpaceSkyEffects(SpaceSkyEffects.Mode.MOON));
        event.register(WhispersOfRlyeh.id("mercury"), new SpaceSkyEffects(SpaceSkyEffects.Mode.MERCURY));
        event.register(WhispersOfRlyeh.id("gas"), new SpaceSkyEffects(SpaceSkyEffects.Mode.GAS));
        event.register(WhispersOfRlyeh.id("saturn"), new SpaceSkyEffects(SpaceSkyEffects.Mode.SATURN));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.DEEP_ONE.get(), DeepOneRenderer::new);
        event.registerEntityRenderer(ModEntities.SHOGGOTH.get(), ShoggothRenderer::new);
    }

    private ClientSetup() {}
}
