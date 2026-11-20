package com.whispersofrlyeh.space;

import com.whispersofrlyeh.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * Terreno de cada planeta, coluna por coluna.
 *
 *  Lua      : planícies de Rocha Lunar cheias de crateras (bordas elevadas, fundo de tufo)
 *  Mercúrio : como a Lua, porém escuro (basalto, tufo, ardósia) e mais esburacado
 *  Marte    : dunas de areia vermelha, camadas de terracota, cânions profundos
 *  Vênus    : basalto e pedra-negra, lagos de lava, névoa amarela (e calor)
 *  Gasosos  : SEM CHÃO. Só camadas de nuvens atravessáveis em várias altitudes
 */
public final class PlanetTerrain {

    private static final SimplexNoise NOISE = new SimplexNoise(RandomSource.create(0xC05A05L));

    private static double n2(double x, double z) {
        return NOISE.getValue(x, z);
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    private static double rand(long seed, int salt) {
        return (mix(seed + salt * 0x9E3779B97F4A7C15L) >>> 11) * 0x1.0p-53;
    }

    public static void generate(Planet planet, ChunkAccess chunk) {
        switch (planet) {
            case MOON, MERCURY -> cratered(planet, chunk);
            case MARS -> mars(chunk);
            case VENUS -> venus(chunk);
            default -> gasGiant(planet, chunk);
        }
    }

    // ---------------------------------------------------------------------
    // Crateras: grade de células; cada célula pode ter uma cratera
    // ---------------------------------------------------------------------

    /** Quanto a altura muda por causa das crateras (negativo = buraco, positivo = borda). */
    private static double craterOffset(int x, int z, int cell, double chance, double rMin, double rMax, long salt) {
        int cellX = Math.floorDiv(x, cell), cellZ = Math.floorDiv(z, cell);
        double offset = 0;
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                long seed = mix((cellX + ox) * 734287L + (cellZ + oz) * 912931L + salt);
                if (rand(seed, 0) > chance) continue;
                double ccx = (cellX + ox) * cell + rand(seed, 1) * cell;
                double ccz = (cellZ + oz) * cell + rand(seed, 2) * cell;
                double r = rMin + rand(seed, 3) * (rMax - rMin);
                double d = Math.sqrt((x - ccx) * (x - ccx) + (z - ccz) * (z - ccz)) / r;
                if (d < 1) offset -= (1 - d * d) * r * 0.45;
                else if (d < 1.35) offset += (1 - (d - 1) / 0.35) * r * 0.12;
            }
        }
        return offset;
    }

    private static boolean nearCraterFloor(int x, int z, int cell, double chance, double rMin, double rMax, long salt) {
        return craterOffset(x, z, cell, chance, rMin, rMax, salt) < -2;
    }

    // ---------------------------------------------------------------------
    // Lua e Mercúrio
    // ---------------------------------------------------------------------

    private static void cratered(Planet planet, ChunkAccess chunk) {
        boolean moon = planet == Planet.MOON;
        long salt = moon ? 0x1111L : 0x2222L;
        ChunkPos cp = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinBuildHeight();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = cp.getMinBlockX() + lx, z = cp.getMinBlockZ() + lz;
                double h = 64 + n2(x * 0.006, z * 0.006) * 8 + n2(x * 0.03, z * 0.03) * 2;
                h += craterOffset(x, z, 40, moon ? 0.55 : 0.75, 5, 22, salt);
                h += craterOffset(x, z, 13, moon ? 0.35 : 0.5, 1.5, 5, salt + 7);
                int top = (int) Math.floor(h);
                boolean floor = nearCraterFloor(x, z, 40, moon ? 0.55 : 0.75, 5, 22, salt);
                double patch = n2(x * 0.02 + 300, z * 0.02);

                for (int y = minY; y <= top; y++) {
                    BlockState s;
                    int depth = top - y;
                    if (y == minY) s = Blocks.BEDROCK.defaultBlockState();
                    else if (moon) {
                        if (depth == 0 && floor) s = Blocks.TUFF.defaultBlockState();
                        else if (depth <= 1 && patch > 0.55) s = Blocks.ANDESITE.defaultBlockState();
                        else if (depth < 8) s = ModBlocks.MOON_ROCK.get().defaultBlockState();
                        else if (depth < 30) s = Blocks.STONE.defaultBlockState();
                        else s = Blocks.DEEPSLATE.defaultBlockState();
                    } else {
                        if (depth == 0) s = patch > 0.3 ? Blocks.BASALT.defaultBlockState()
                                : floor ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.TUFF.defaultBlockState();
                        else if (depth < 6) s = Blocks.TUFF.defaultBlockState();
                        else s = Blocks.DEEPSLATE.defaultBlockState();
                    }
                    chunk.setBlockState(pos.set(x, y, z), s, false);
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Marte
    // ---------------------------------------------------------------------

    private static final BlockState[] MARS_STRATA = {
            Blocks.RED_TERRACOTTA.defaultBlockState(), Blocks.ORANGE_TERRACOTTA.defaultBlockState(),
            Blocks.TERRACOTTA.defaultBlockState(), Blocks.RED_TERRACOTTA.defaultBlockState(),
            Blocks.BROWN_TERRACOTTA.defaultBlockState(), Blocks.ORANGE_TERRACOTTA.defaultBlockState(),
            Blocks.RED_TERRACOTTA.defaultBlockState(), Blocks.LIGHT_GRAY_TERRACOTTA.defaultBlockState(),
    };

    private static void mars(ChunkAccess chunk) {
        ChunkPos cp = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinBuildHeight();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = cp.getMinBlockX() + lx, z = cp.getMinBlockZ() + lz;
                double h = 66 + n2(x * 0.004, z * 0.004) * 14 + n2(x * 0.02, z * 0.02) * 3;
                h += Math.abs(n2(x * 0.05 + 100, z * 0.012)) * 2.5;                       // dunas
                double ridge = 1 - Math.abs(n2(x * 0.0025 + 500, z * 0.0025));
                if (ridge > 0.9) h -= (ridge - 0.9) / 0.1 * 42;                             // cânions
                h += craterOffset(x, z, 64, 0.25, 6, 20, 0x3333L);
                int top = (int) Math.floor(h);

                for (int y = minY; y <= top; y++) {
                    int depth = top - y;
                    BlockState s;
                    if (y == minY) s = Blocks.BEDROCK.defaultBlockState();
                    else if (depth == 0) s = Blocks.RED_SAND.defaultBlockState();
                    else if (depth < 4) s = Blocks.RED_SANDSTONE.defaultBlockState();
                    else if (y < 20) s = Blocks.BASALT.defaultBlockState();
                    else s = MARS_STRATA[Math.floorMod(y / 3, MARS_STRATA.length)];
                    chunk.setBlockState(pos.set(x, y, z), s, false);
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Vênus
    // ---------------------------------------------------------------------

    private static void venus(ChunkAccess chunk) {
        ChunkPos cp = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinBuildHeight();
        int lavaLevel = 56;

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = cp.getMinBlockX() + lx, z = cp.getMinBlockZ() + lz;
                double h = 64 + n2(x * 0.005, z * 0.005) * 16 + n2(x * 0.03, z * 0.03) * 3;
                double volcano = n2(x * 0.0015 + 900, z * 0.0015);
                if (volcano > 0.6) h += (volcano - 0.6) / 0.4 * 50;                         // vulcões
                int top = (int) Math.floor(h);
                double patch = n2(x * 0.04, z * 0.04 + 77);

                for (int y = minY; y <= Math.max(top, lavaLevel); y++) {
                    BlockState s;
                    int depth = top - y;
                    if (y == minY) s = Blocks.BEDROCK.defaultBlockState();
                    else if (y > top) s = Blocks.LAVA.defaultBlockState();                   // lagos de lava
                    else if (depth == 0) s = top <= lavaLevel + 1 ? Blocks.MAGMA_BLOCK.defaultBlockState()
                            : patch > 0.2 ? Blocks.BASALT.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState();
                    else if (depth < 5) s = Blocks.BLACKSTONE.defaultBlockState();
                    else s = Blocks.DEEPSLATE.defaultBlockState();
                    chunk.setBlockState(pos.set(x, y, z), s, false);
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Gigantes gasosos: só nuvens, nenhum chão
    // ---------------------------------------------------------------------

    /** y, espessura máxima, escala do ruído, limiar */
    private static final double[][] CLOUD_LAYERS = {
            {440, 3, 0.012, 0.25}, {380, 4, 0.010, 0.15}, {320, 5, 0.008, 0.10},
            {250, 6, 0.007, 0.05}, {180, 8, 0.006, 0.00}, {110, 10, 0.005, -0.10}, {45, 12, 0.004, -0.25},
    };

    private static Block[] palette(Planet planet) {
        return switch (planet) {
            case JUPITER -> new Block[]{ModBlocks.CLOUD_CREAM.get(), ModBlocks.CLOUD_ORANGE.get(), ModBlocks.CLOUD_BROWN.get(),
                    ModBlocks.CLOUD_CREAM.get(), ModBlocks.CLOUD_ORANGE.get(), ModBlocks.CLOUD_BROWN.get(), ModBlocks.CLOUD_BROWN.get()};
            case SATURN -> new Block[]{ModBlocks.CLOUD_CREAM.get(), ModBlocks.CLOUD_GOLD.get(), ModBlocks.CLOUD_CREAM.get(),
                    ModBlocks.CLOUD_GOLD.get(), ModBlocks.CLOUD_ORANGE.get(), ModBlocks.CLOUD_GOLD.get(), ModBlocks.CLOUD_BROWN.get()};
            case URANUS -> new Block[]{ModBlocks.CLOUD_CYAN.get(), ModBlocks.CLOUD_CYAN.get(), ModBlocks.CLOUD_CREAM.get(),
                    ModBlocks.CLOUD_CYAN.get(), ModBlocks.CLOUD_BLUE.get(), ModBlocks.CLOUD_CYAN.get(), ModBlocks.CLOUD_BLUE.get()};
            default -> new Block[]{ModBlocks.CLOUD_BLUE.get(), ModBlocks.CLOUD_CYAN.get(), ModBlocks.CLOUD_BLUE.get(),
                    ModBlocks.CLOUD_BLUE.get(), ModBlocks.CLOUD_CYAN.get(), ModBlocks.CLOUD_BLUE.get(), ModBlocks.CLOUD_BLUE.get()};
        };
    }

    private static void gasGiant(Planet planet, ChunkAccess chunk) {
        ChunkPos cp = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Block[] pal = palette(planet);
        int maxY = chunk.getMaxBuildHeight() - 1, minY = chunk.getMinBuildHeight();
        double offset = planet.ordinal() * 1000;

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = cp.getMinBlockX() + lx, z = cp.getMinBlockZ() + lz;
                for (int i = 0; i < CLOUD_LAYERS.length; i++) {
                    double[] L = CLOUD_LAYERS[i];
                    double sc = L[2];
                    // nuvens esticadas no eixo X, como faixas de vento
                    double n = n2(x * sc * 0.4 + offset + i * 97, z * sc * 1.6) * 0.7 + n2(x * sc * 3, z * sc * 3 + i * 31) * 0.3;
                    if (n <= L[3]) continue;
                    int thick = (int) Math.ceil(Math.min(1.0, (n - L[3]) * 2.5) * L[1]);
                    int baseY = (int) L[0] + (int) (n2(x * 0.01 + i, z * 0.01) * 4);
                    Block block = pal[i];
                    if (planet == Planet.JUPITER && n > L[3] + 0.45) block = ModBlocks.CLOUD_BROWN.get(); // tempestades escuras
                    for (int y = baseY - thick / 2; y <= baseY + thick / 2; y++) {
                        if (y < minY || y > maxY) continue;
                        chunk.setBlockState(pos.set(x, y, z), block.defaultBlockState(), false);
                    }
                }
            }
        }
    }

    private PlanetTerrain() {}
}
