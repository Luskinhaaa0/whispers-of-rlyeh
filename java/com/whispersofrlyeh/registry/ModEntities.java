package com.whispersofrlyeh.registry;

import com.whispersofrlyeh.WhispersOfRlyeh;
import com.whispersofrlyeh.entity.DeepOne;
import com.whispersofrlyeh.entity.Shoggoth;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, WhispersOfRlyeh.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<DeepOne>> DEEP_ONE = ENTITY_TYPES.register("deep_one",
            () -> EntityType.Builder.of(DeepOne::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .eyeHeight(1.74f)
                    .clientTrackingRange(8)
                    .build("deep_one"));

    public static final DeferredHolder<EntityType<?>, EntityType<Shoggoth>> SHOGGOTH = ENTITY_TYPES.register("shoggoth",
            () -> EntityType.Builder.of(Shoggoth::new, MobCategory.MONSTER)
                    .sized(2.6f, 2.4f)
                    .eyeHeight(1.8f)
                    .clientTrackingRange(10)
                    .build("shoggoth"));

    private ModEntities() {}
}
