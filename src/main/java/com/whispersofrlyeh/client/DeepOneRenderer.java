package com.whispersofrlyeh.client;

import com.whispersofrlyeh.WhispersOfRlyeh;
import com.whispersofrlyeh.entity.DeepOne;
import net.minecraft.client.model.DrownedModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

/** Usa o modelo do Afogado com a textura escamosa do Deep One. */
public class DeepOneRenderer extends AbstractZombieRenderer<DeepOne, DrownedModel<DeepOne>> {
    private static final ResourceLocation TEXTURE = WhispersOfRlyeh.id("textures/entity/deep_one.png");

    public DeepOneRenderer(EntityRendererProvider.Context context) {
        super(context,
                new DrownedModel<>(context.bakeLayer(ModelLayers.DROWNED)),
                new DrownedModel<>(context.bakeLayer(ModelLayers.DROWNED_INNER_ARMOR)),
                new DrownedModel<>(context.bakeLayer(ModelLayers.DROWNED_OUTER_ARMOR)));
    }

    @Override
    public ResourceLocation getTextureLocation(Zombie entity) {
        return TEXTURE;
    }
}
