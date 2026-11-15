package com.whispersofrlyeh.space;

import com.whispersofrlyeh.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.List;

/**
 * O Sistema Solar do mod: posições fixas, tamanhos e "receita" de blocos de cada astro.
 *
 *   Astro     Centro (x, y, z)        Raio
 *   Sol       (-1300, 180, -1300)     110
 *   Terra     (  260, 150,   260)      45
 *   Lua       (  360, 185,   330)      14
 *   Marte     ( -650, 150,   520)      26
 *   Júpiter   (  950, 170,  -800)      80
 *
 * A mesma função blockFor() é usada pelo gerador de chunks (blocos reais) e pelo céu
 * (para pintar o disco do planeta distante com as cores dos blocos de verdade).
 */
public final class SpaceBodies {

    public enum Kind { SUN, EARTH, MOON, MARS, JUPITER }

    public record Body(Kind kind, String name, int cx, int cy, int cz, int radius) {
        public double distanceTo(double x, double y, double z) {
            double dx = x - cx, dy = y - cy, dz = z - cz;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }

    public static final Body SUN = new Body(Kind.SUN, "sun", -1300, 180, -1300, 110);
    public static final Body EARTH = new Body(Kind.EARTH, "earth", 260, 150, 260, 45);
    public static final Body MOON = new Body(Kind.MOON, "moon", 360, 185, 330, 14);
    public static final Body MARS = new Body(Kind.MARS, "mars", -650, 150, 520, 26);
    public static final Body JUPITER = new Body(Kind.JUPITER, "jupiter", 950, 170, -800, 80);

    public static final List<Body> BODIES = List.of(SUN, EARTH, MOON, MARS, JUPITER);

    private static final SimplexNoise NOISE = new SimplexNoise(RandomSource.create(0x5A11E7L));

    /** Crateras da Lua: direção (x, y, z) na superfície + raio da cratera. */
    private static final double[][] MOON_CRATERS = makeCraters(18, 0xC0FFEEL);

    private static double[][] makeCraters(int count, long seed) {
        RandomSource r = RandomSource.create(seed);
        double[][] out = new double[count][4];
        for (int i = 0; i < count; i++) {
            double x = r.nextGaussian(), y = r.nextGaussian(), z = r.nextGaussian();
            double len = Math.sqrt(x * x + y * y + z * z);
            out[i][0] = x / len;
            out[i][1] = y / len;
            out[i][2] = z / len;
            out[i][3] = 2.5 + r.nextDouble() * 3.5;
        }
        return out;
    }

    // ---------------------------------------------------------------------
    // Geração dos blocos de um chunk
    // ---------------------------------------------------------------------

    public static void generate(ChunkAccess chunk) {
        ChunkPos cp = chunk.getPos();
        int x0 = cp.getMinBlockX();
        int z0 = cp.getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight() - 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (Body b : BODIES) {
            int r = b.radius();
            if (x0 + 15 < b.cx() - r || x0 > b.cx() + r || z0 + 15 < b.cz() - r || z0 > b.cz() + r) continue;

            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    int x = x0 + lx, z = z0 + lz;
                    double dx = x - b.cx(), dz = z - b.cz();
                    double h2 = (double) r * r - dx * dx - dz * dz;
                    if (h2 < 0) continue;
                    int h = (int) Math.floor(Math.sqrt(h2));
                    int yStart = Math.max(minY, b.cy() - h);
                    int yEnd = Math.min(maxY, b.cy() + h);
                    for (int y = yStart; y <= yEnd; y++) {
                        BlockState state = blockFor(b, x, y, z);
                        if (state != null) {
                            chunk.setBlockState(pos.set(x, y, z), state, false);
                        }
                    }
                }
            }
        }
    }

    /** Qual bloco existe nesta posição do astro (ou null = vazio). */
    public static BlockState blockFor(Body b, double x, double y, double z) {
        double dx = x - b.cx(), dy = y - b.cy(), dz = z - b.cz();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > b.radius()) return null;
        if (dist < 0.001) dist = 0.001;
        double nx = dx / dist, ny = dy / dist, nz = dz / dist;
        double depth = b.radius() - dist; // 0 = superfície

        return switch (b.kind()) {
            case SUN -> sun(x, y, z, depth);
            case EARTH -> earth(nx, ny, nz, depth, b.radius());
            case MOON -> moon(x, y, z, nx, ny, nz, depth, b);
            case MARS -> mars(nx, ny, nz, depth);
            case JUPITER -> jupiter(nx, ny, nz, depth);
        };
    }

    private static double n3(double x, double y, double z) {
        return NOISE.getValue(x, y, z);
    }

    // ----- Sol: magma pulsante, manchas brilhantes -----
    private static BlockState sun(double x, double y, double z, double depth) {
        if (depth < 1.5) {
            double n = n3(x * 0.07, y * 0.07, z * 0.07) + 0.4 * n3(x * 0.2, y * 0.2, z * 0.2);
            if (n > 0.75) return Blocks.SHROOMLIGHT.defaultBlockState();
            if (n > 0.55) return Blocks.OCHRE_FROGLIGHT.defaultBlockState();
            if (n < -0.85) return Blocks.GLOWSTONE.defaultBlockState();
            if (n < -0.55) return Blocks.ORANGE_CONCRETE.defaultBlockState();
            return Blocks.MAGMA_BLOCK.defaultBlockState();
        }
        return depth < 8 ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.ORANGE_TERRACOTTA.defaultBlockState();
    }

    // ----- Terra: continentes, oceanos (água presa sob vidro azul), polos e núcleo -----
    private static BlockState earth(double nx, double ny, double nz, double depth, int radius) {
        double c = n3(nx * 2.2, ny * 2.2, nz * 2.2) + 0.5 * n3(nx * 5 + 10, ny * 5, nz * 5);
        boolean land = c > 0.05;
        boolean polar = Math.abs(ny) > 0.82 + 0.05 * n3(nx * 6, ny * 6 + 3, nz * 6);

        if (depth > radius - 6) return Blocks.MAGMA_BLOCK.defaultBlockState();   // núcleo
        if (depth > 14) return Blocks.DEEPSLATE.defaultBlockState();

        if (polar) {
            if (depth < 1) return Blocks.SNOW_BLOCK.defaultBlockState();
            if (depth < 4) return Blocks.PACKED_ICE.defaultBlockState();
            return Blocks.STONE.defaultBlockState();
        }
        if (land) {
            boolean desert = Math.abs(ny) < 0.3 && c > 0.35;
            boolean mountain = c > 0.7;
            if (depth < 1) {
                if (mountain) return Blocks.STONE.defaultBlockState();
                return desert ? Blocks.SANDSTONE.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
            }
            if (depth < 4) return desert ? Blocks.SANDSTONE.defaultBlockState() : Blocks.DIRT.defaultBlockState();
            return Blocks.STONE.defaultBlockState();
        }
        // Oceano: superfície de vidro azul segura a água (nada vaza para o espaço)
        if (depth < 1.8) return Blocks.BLUE_STAINED_GLASS.defaultBlockState();
        if (depth < 3.5) return Blocks.WATER.defaultBlockState();
        if (depth < 4.5) return Blocks.CLAY.defaultBlockState();
        return Blocks.STONE.defaultBlockState();
    }

    // ----- Lua: rocha lunar com crateras -----
    private static BlockState moon(double x, double y, double z, double nx, double ny, double nz, double depth, Body b) {
        for (double[] c : MOON_CRATERS) {
            double cr = c[3];
            double ccx = b.cx() + c[0] * (b.radius() + cr * 0.35);
            double ccy = b.cy() + c[1] * (b.radius() + cr * 0.35);
            double ccz = b.cz() + c[2] * (b.radius() + cr * 0.35);
            double ddx = x - ccx, ddy = y - ccy, ddz = z - ccz;
            double d2 = ddx * ddx + ddy * ddy + ddz * ddz;
            if (d2 < cr * cr) return null;                                       // buraco da cratera
            if (d2 < (cr + 1.2) * (cr + 1.2) && depth < 2) return Blocks.TUFF.defaultBlockState(); // borda escura
        }
        double n = n3(x * 0.15, y * 0.15, z * 0.15);
        if (depth < 1.5) {
            if (n > 0.6) return Blocks.ANDESITE.defaultBlockState();
            return ModBlocks.MOON_ROCK.get().defaultBlockState();
        }
        return depth > 9 ? Blocks.DEEPSLATE.defaultBlockState() : ModBlocks.MOON_ROCK.get().defaultBlockState();
    }

    // ----- Marte: areia vermelha, terracota, cânions e polos de gelo -----
    private static BlockState mars(double nx, double ny, double nz, double depth) {
        double n = n3(nx * 4, ny * 4, nz * 4);
        double canyon = Math.abs(n3(nx * 1.5 + 50, ny * 1.5, nz * 1.5));
        if (canyon < 0.06 && depth < 5 && Math.abs(ny) < 0.5) return null; // Valles Marineris

        if (ny > 0.88) return depth < 1 ? Blocks.SNOW_BLOCK.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState();
        if (ny < -0.88 && depth < 3) return Blocks.PACKED_ICE.defaultBlockState();

        if (depth < 1) {
            // Areia só no hemisfério de cima e longe dos cânions, para não cair no espaço
            if (ny > 0.45 && canyon > 0.12) return Blocks.RED_SAND.defaultBlockState();
            return n > 0 ? Blocks.RED_TERRACOTTA.defaultBlockState() : Blocks.ORANGE_TERRACOTTA.defaultBlockState();
        }
        if (depth < 4) return n > 0.2 ? Blocks.RED_SANDSTONE.defaultBlockState() : Blocks.TERRACOTTA.defaultBlockState();
        return depth > 12 ? Blocks.BASALT.defaultBlockState() : Blocks.RED_TERRACOTTA.defaultBlockState();
    }

    // ----- Júpiter: faixas de nuvens e a Grande Mancha Vermelha -----
    private static final BlockState[] JUPITER_BANDS = {
            Blocks.WHITE_TERRACOTTA.defaultBlockState(),
            Blocks.ORANGE_TERRACOTTA.defaultBlockState(),
            Blocks.BROWN_TERRACOTTA.defaultBlockState(),
            Blocks.YELLOW_TERRACOTTA.defaultBlockState(),
            Blocks.WHITE_TERRACOTTA.defaultBlockState(),
            Blocks.LIGHT_GRAY_TERRACOTTA.defaultBlockState(),
            Blocks.ORANGE_TERRACOTTA.defaultBlockState(),
            Blocks.TERRACOTTA.defaultBlockState(),
    };

    private static BlockState jupiter(double nx, double ny, double nz, double depth) {
        if (depth > 3) return Blocks.BROWN_TERRACOTTA.defaultBlockState();
        double lon = Math.atan2(nz, nx);
        double spotLat = (ny + 0.35) / 0.1;
        double spotLon = (lon - 0.8) / 0.25;
        double spot = spotLat * spotLat + spotLon * spotLon;
        if (spot < 0.45) return Blocks.RED_CONCRETE.defaultBlockState();
        if (spot < 1.0) return Blocks.RED_TERRACOTTA.defaultBlockState();

        double lat = ny + n3(nx * 3, ny * 3, nz * 3) * 0.05 + n3(nx * 9, ny * 2, nz * 9) * 0.02;
        int band = (int) Math.floor((lat + 1) * 9);
        return JUPITER_BANDS[Math.floorMod(band, JUPITER_BANDS.length)];
    }

    private SpaceBodies() {}
}
