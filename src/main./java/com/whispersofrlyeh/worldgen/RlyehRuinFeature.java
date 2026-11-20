package com.whispersofrlyeh.worldgen;

import com.mojang.serialization.Codec;
import com.whispersofrlyeh.WhispersOfRlyeh;
import com.whispersofrlyeh.block.IdolBlock;
import com.whispersofrlyeh.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Ruína de R'lyeh: plataforma irregular de pedra ciclópica no fundo do oceano profundo,
 * pilares tortos ("geometria errada"), um altar com o Ídolo de Cthulhu e um baú com tesouros.
 */
public class RlyehRuinFeature extends Feature<NoneFeatureConfiguration> {

    public static final ResourceKey<LootTable> LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, WhispersOfRlyeh.id("chests/rlyeh_ruin"));

    public RlyehRuinFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        // Só no fundo de água funda
        if (!level.getFluidState(origin).is(FluidTags.WATER) || !level.getFluidState(origin.above(6)).is(FluidTags.WATER)) {
            return false;
        }

        BlockPos floor = origin.below();
        int radius = 4 + random.nextInt(2);

        // Plataforma irregular
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius + random.nextInt(3)) continue;
                if (random.nextFloat() > 0.88f) continue; // buracos de erosão
                BlockPos p = floor.offset(dx, 0, dz);
                level.setBlock(p, stone(random), 2);
                // fundação até tocar o chão
                for (int dy = 1; dy <= 4; dy++) {
                    BlockPos below = p.below(dy);
                    if (level.getBlockState(below).isSolid()) break;
                    level.setBlock(below, stone(random), 2);
                }
            }
        }

        // Pilares tortos nos cantos
        int[][] corners = {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}};
        for (int[] c : corners) {
            if (random.nextFloat() < 0.2f) continue; // alguns caíram
            int height = 3 + random.nextInt(6);
            int leanX = random.nextInt(3) - 1;
            int leanZ = random.nextInt(3) - 1;
            for (int y = 1; y <= height; y++) {
                int ox = c[0] + (y * leanX) / 3;
                int oz = c[1] + (y * leanZ) / 3;
                level.setBlock(floor.offset(ox, y, oz), pillar(random), 2);
            }
        }

        // Altar com o ídolo
        level.setBlock(floor.above(), Blocks.DARK_PRISMARINE.defaultBlockState(), 2);
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        level.setBlock(floor.above(2), ModBlocks.CTHULHU_IDOL.get().defaultBlockState().setValue(IdolBlock.FACING, facing), 2);

        // Luzes estranhas
        level.setBlock(floor.offset(2, 1, -2), Blocks.SEA_LANTERN.defaultBlockState(), 2);
        level.setBlock(floor.offset(-2, 1, 2), Blocks.SEA_LANTERN.defaultBlockState(), 2);

        // Baú com tesouros
        BlockPos chestPos = floor.offset(0, 1, 2);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState()
                .setValue(ChestBlock.WATERLOGGED, true)
                .setValue(ChestBlock.FACING, Direction.SOUTH), 2);
        RandomizableContainer.setBlockEntityLootTable(level, random, chestPos, LOOT);

        return true;
    }

    private static BlockState stone(RandomSource random) {
        float f = random.nextFloat();
        if (f < 0.6f) return ModBlocks.CYCLOPEAN_STONE.get().defaultBlockState();
        if (f < 0.8f) return Blocks.DARK_PRISMARINE.defaultBlockState();
        if (f < 0.9f) return Blocks.PRISMARINE_BRICKS.defaultBlockState();
        return Blocks.PRISMARINE.defaultBlockState();
    }

    private static BlockState pillar(RandomSource random) {
        return random.nextFloat() < 0.75f
                ? ModBlocks.CYCLOPEAN_STONE.get().defaultBlockState()
                : Blocks.DARK_PRISMARINE.defaultBlockState();
    }
}
