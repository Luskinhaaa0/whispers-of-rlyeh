package com.whispersofrlyeh.registry;

import com.whispersofrlyeh.WhispersOfRlyeh;
import com.whispersofrlyeh.block.CloudBlock;
import com.whispersofrlyeh.block.IdolBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
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

    // Nuvens dos gigantes gasosos (atravessáveis, translúcidas)
    public static final DeferredBlock<CloudBlock> CLOUD_CREAM = cloud("cloud_cream", MapColor.SAND);
    public static final DeferredBlock<CloudBlock> CLOUD_ORANGE = cloud("cloud_orange", MapColor.COLOR_ORANGE);
    public static final DeferredBlock<CloudBlock> CLOUD_BROWN = cloud("cloud_brown", MapColor.COLOR_BROWN);
    public static final DeferredBlock<CloudBlock> CLOUD_GOLD = cloud("cloud_gold", MapColor.GOLD);
    public static final DeferredBlock<CloudBlock> CLOUD_CYAN = cloud("cloud_cyan", MapColor.COLOR_CYAN);
    public static final DeferredBlock<CloudBlock> CLOUD_BLUE = cloud("cloud_blue", MapColor.COLOR_BLUE);

    private static DeferredBlock<CloudBlock> cloud(String name, MapColor color) {
        return BLOCKS.register(name, () -> new CloudBlock(BlockBehaviour.Properties.of()
                .mapColor(color)
                .noCollission()
                .noOcclusion()
                .noLootTable()
                .instabreak()
                .replaceable()
                .pushReaction(PushReaction.DESTROY)
                .isViewBlocking((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .sound(SoundType.WOOL)));
    }

    private ModBlocks() {}
}
