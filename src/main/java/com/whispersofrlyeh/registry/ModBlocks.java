package com.whispersofrlyeh.registry;

import com.whispersofrlyeh.WhispersOfRlyeh;
import com.whispersofrlyeh.block.IdolBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(WhispersOfRlyeh.MODID);

    /** Pedra das ruínas de R'lyeh. */
    public static final DeferredBlock<Block> CYCLOPEAN_STONE = BLOCKS.register("cyclopean_stone",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WARPED_STEM)
                    .strength(3.0f, 9.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE)));

    /** Estatueta de Cthulhu: drena sanidade por perto, mas dá sonhos reveladores. */
    public static final DeferredBlock<IdolBlock> CTHULHU_IDOL = BLOCKS.register("cthulhu_idol",
            () -> new IdolBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(2.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(state -> 3)
                    .sound(SoundType.STONE)));

    /** Rocha Lunar: superfície da Lua. */
    public static final DeferredBlock<Block> MOON_ROCK = BLOCKS.register("moon_rock",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.5f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.TUFF)));

    private ModBlocks() {}
}
