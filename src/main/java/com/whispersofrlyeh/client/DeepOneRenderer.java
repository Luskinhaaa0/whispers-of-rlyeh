package com.whispersofrlyeh.client;

import com.whispersofrlyeh.WhispersOfRlyeh;
import com.whispersofrlyeh.entity.DeepOne;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderiza o Deep One com o modelo 3D do GeckoLib. Os arquivos ficam em:
 *   assets/whispersofrlyeh/geo/entity/deep_one.geo.json            (modelo, abre no Blockbench)
 *   assets/whispersofrlyeh/animations/entity/deep_one.animation.json (animações)
 *   assets/whispersofrlyeh/textures/entity/deep_one.png              (textura)
 * O "true" faz a cabeça (osso "head") seguir o olhar da criatura.
 */
public class DeepOneRenderer extends GeoEntityRenderer<DeepOne> {

    public DeepOneRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(WhispersOfRlyeh.id("deep_one"), true));
        this.shadowRadius = 0.5f;
    }
}
