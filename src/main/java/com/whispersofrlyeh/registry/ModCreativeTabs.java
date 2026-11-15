package com.whispersofrlyeh.registry;

import com.whispersofrlyeh.WhispersOfRlyeh;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WhispersOfRlyeh.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.whispersofrlyeh"))
                    .icon(() -> new ItemStack(ModItems.NECRONOMICON.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.NECRONOMICON.get());
                        output.accept(ModItems.ELDER_SIGN.get());
                        output.accept(ModItems.CALMING_TEA.get());
                        output.accept(ModItems.CTHULHU_IDOL.get());
                        output.accept(ModItems.CYCLOPEAN_STONE.get());
                        output.accept(ModItems.DEEP_ONE_SPAWN_EGG.get());
                    })
                    .build());

    private ModCreativeTabs() {}
}
