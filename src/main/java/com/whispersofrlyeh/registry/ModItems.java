package com.whispersofrlyeh.registry;

import com.whispersofrlyeh.WhispersOfRlyeh;
import com.whispersofrlyeh.item.CalmingTeaItem;
import com.whispersofrlyeh.item.ElderSignItem;
import com.whispersofrlyeh.item.NecronomiconItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WhispersOfRlyeh.MODID);

    public static final DeferredItem<NecronomiconItem> NECRONOMICON = ITEMS.register("necronomicon",
            () -> new NecronomiconItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<ElderSignItem> ELDER_SIGN = ITEMS.register("elder_sign",
            () -> new ElderSignItem(new Item.Properties().durability(64).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<CalmingTeaItem> CALMING_TEA = ITEMS.register("calming_tea",
            () -> new CalmingTeaItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<BlockItem> CYCLOPEAN_STONE =
            ITEMS.registerSimpleBlockItem("cyclopean_stone", ModBlocks.CYCLOPEAN_STONE);

    public static final DeferredItem<BlockItem> CTHULHU_IDOL =
            ITEMS.registerSimpleBlockItem("cthulhu_idol", ModBlocks.CTHULHU_IDOL);

    public static final DeferredItem<BlockItem> MOON_ROCK =
            ITEMS.registerSimpleBlockItem("moon_rock", ModBlocks.MOON_ROCK);

    public static final DeferredItem<DeferredSpawnEggItem> DEEP_ONE_SPAWN_EGG = ITEMS.register("deep_one_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.DEEP_ONE, 0x1F3D2B, 0x7FC79A, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> SHOGGOTH_SPAWN_EGG = ITEMS.register("shoggoth_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.SHOGGOTH, 0x0B0F0C, 0xC8FF5A, new Item.Properties()));

    private ModItems() {}
}
