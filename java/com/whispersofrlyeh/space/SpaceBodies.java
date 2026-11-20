package com.whispersofrlyeh.space;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * O que existe fisicamente na dimensão do Espaço: só o Cinturão de Asteroides
 * (uma faixa entre Z 6000 e Z 7600, entre Marte e Júpiter), com rochas de 2 a 7 blocos
 * de raio e minérios. O resto (Sol, Terra, planetas, cometas) é desenhado no céu.
 */
public final class SpaceBodies {

    /** O Sol (só visual + calor). */
    public static final int SUN_X = 0, SUN_Y = 400, SUN_Z = -18000, SUN_RADIUS = 3000;

    /** A Terra visual fica sempre embaixo do jogador: centro em Y -2050, raio 2000 (superfície em Y -50). */
    public static final int EARTH_CENTER_Y = -2050, EARTH_RADIUS = 2000;

    public static final int BELT_MIN_Z = 6000, BELT_MAX_Z = 7600;

    public static double sunDistance(double x, double y, double z) {
        double dx = x - SUN_X, dy = y - SUN_Y, dz = z - SUN_Z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // ---------------------------------------------------------------------
    // Cinturão de asteroides
    // ---------------------------------------------------------------------

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    private static double rand(long seed, int salt) {
        return (mix(seed + salt * 0x9E3779B97F4A7C15L) >>> 11) * 0x1.0p-53;
    }

    public static void generate(ChunkAccess chunk) {
        ChunkPos cp = chunk.getPos();
        int minZ = cp.getMinBlockZ();
        if (minZ + 32 < BELT_MIN_Z || minZ - 16 > BELT_MAX_Z) return;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinBuildHeight(), maxY = chunk.getMaxBuildHeight() - 1;

        // Asteroides centrados no próprio chunk ou nos 8 vizinhos podem invadir este chunk
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                int ax = cp.x + ox, az = cp.z + oz;
                long seed = mix(ax * 341873128712L + az * 132897987541L + 0xA57E801DL);
                if (rand(seed, 0) > 0.12) continue;

                double cx = ax * 16 + rand(seed, 1) * 16;
                double cz = az * 16 + rand(seed, 2) * 16;
                if (cz < BELT_MIN_Z || cz > BELT_MAX_Z) continue;
                double cy = 40 + rand(seed, 3) * 340;
                double r = 2 + rand(seed, 4) * 5;

                int x0 = cp.getMinBlockX(), z0 = cp.getMinBlockZ();
                for (int x = Math.max(x0, (int) Math.floor(cx - r)); x <= Math.min(x0 + 15, (int) Math.ceil(cx + r)); x++) {
                    for (int z = Math.max(z0, (int) Math.floor(cz - r)); z <= Math.min(z0 + 15, (int) Math.ceil(cz + r)); z++) {
                        for (int y = Math.max(minY, (int) Math.floor(cy - r)); y <= Math.min(maxY, (int) Math.ceil(cy + r)); y++) {
                            double dx = x + 0.5 - cx, dy = y + 0.5 - cy, dz = z + 0.5 - cz;
                            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                            // superfície irregular
                            double lumpy = r * (0.8 + 0.25 * rand(mix(seed + x * 31L + y * 17L + z * 13L), 9));
                            if (d > lumpy) continue;
                            chunk.setBlockState(pos.set(x, y, z), asteroidBlock(seed, x, y, z, d, r), false);
                        }
                    }
                }
            }
        }
    }

    private static BlockState asteroidBlock(long seed, int x, int y, int z, double d, double r) {
        double roll = rand(mix(seed ^ (x * 73856093L) ^ (y * 19349663L) ^ (z * 83492791L)), 5);
        boolean metallic = rand(seed, 6) < 0.25; // alguns asteroides são ricos em metal
        if (d < r * 0.6) {
            if (roll < 0.004) return Blocks.DIAMOND_ORE.defaultBlockState();
            if (roll < (metallic ? 0.08 : 0.03)) return Blocks.DEEPSLATE_GOLD_ORE.defaultBlockState();
            if (roll < (metallic ? 0.25 : 0.09)) return Blocks.DEEPSLATE_IRON_ORE.defaultBlockState();
            if (roll < 0.14) return Blocks.DEEPSLATE_COPPER_ORE.defaultBlockState();
        }
        if (roll < 0.5) return Blocks.TUFF.defaultBlockState();
        if (roll < 0.8) return metallic ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
        return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
    }

    private SpaceBodies() {}
}
