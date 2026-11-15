package com.whispersofrlyeh.registry;

import com.whispersofrlyeh.WhispersOfRlyeh;
import com.whispersofrlyeh.worldgen.RlyehRuinFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, WhispersOfRlyeh.MODID);

    public static final DeferredHolder<Feature<?>, RlyehRuinFeature> RLYEH_RUIN = FEATURES.register("rlyeh_ruin",
            () -> new RlyehRuinFeature(NoneFeatureConfiguration.CODEC));

    private ModFeatures() {}
}
