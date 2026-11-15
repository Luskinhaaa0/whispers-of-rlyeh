package com.whispersofrlyeh;

import com.whispersofrlyeh.entity.DeepOne;
import com.whispersofrlyeh.registry.ModEntities;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/** Eventos do "mod bus": atributos e regras de spawn das criaturas. */
@EventBusSubscriber(modid = WhispersOfRlyeh.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModEvents {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.DEEP_ONE.get(), DeepOne.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.DEEP_ONE.get(),
                SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                DeepOne::checkSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private ModEvents() {}
}
