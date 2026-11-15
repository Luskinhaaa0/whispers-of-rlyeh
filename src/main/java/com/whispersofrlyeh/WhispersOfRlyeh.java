package com.whispersofrlyeh;

import com.mojang.logging.LogUtils;
import com.whispersofrlyeh.network.ModNetworking;
import com.whispersofrlyeh.registry.ModAttachments;
import com.whispersofrlyeh.registry.ModBlocks;
import com.whispersofrlyeh.registry.ModCreativeTabs;
import com.whispersofrlyeh.registry.ModDimensions;
import com.whispersofrlyeh.registry.ModEntities;
import com.whispersofrlyeh.registry.ModFeatures;
import com.whispersofrlyeh.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Classe principal do mod. O NeoForge cria uma instância dela quando o jogo carrega.
 */
@Mod(WhispersOfRlyeh.MODID)
public class WhispersOfRlyeh {
    public static final String MODID = "whispersofrlyeh";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WhispersOfRlyeh(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModFeatures.FEATURES.register(modEventBus);
        ModDimensions.CHUNK_GENERATORS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);

        // Pacotes de rede (servidor -> cliente) para sincronizar a sanidade
        modEventBus.addListener(ModNetworking::register);

        LOGGER.info("Ph'nglui mglw'nafh Cthulhu R'lyeh wgah'nagl fhtagn.");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
