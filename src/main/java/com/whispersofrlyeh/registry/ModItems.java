package com.whispersofrlyeh.registry;

import com.whispersofrlyeh.WhispersOfRlyeh;
import com.whispersofrlyeh.item.ElderSignItem;
import com.whispersofrlyeh.item.NecronomiconItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WhispersOfRlyeh.MODID);

    public static final DeferredItem<NecronomiconItem> NECRONOMICON = ITEMS.register("necronomicon",
            () -> new NecronomiconItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<ElderSignItem> ELDER_SIGN = ITEMS.register("elder_sign",
            () -> new ElderSignItem(new Item.Properties().durability(64).rarity(Rarity.UNCOMMON)));

    private ModItems() {}
}
