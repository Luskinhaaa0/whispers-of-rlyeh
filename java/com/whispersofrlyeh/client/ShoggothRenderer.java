package com.whispersofrlyeh.client;

import com.whispersofrlyeh.WhispersOfRlyeh;
import com.whispersofrlyeh.entity.Shoggoth;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Renderiza o Shoggoth com o modelo 3D do GeckoLib.
 * A camada "glowmask" faz os olhos brilharem no escuro
 * (textures/entity/shoggoth_glowmask.png).
 */
public class ShoggothRenderer extends GeoEntityRenderer<Shoggoth> {

    public ShoggothRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(WhispersOfRlyeh.id("shoggoth")));
        this.shadowRadius = 1.7f;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
