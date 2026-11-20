package com.whispersofrlyeh.registry;

import com.mojang.serialization.MapCodec;
import com.whispersofrlyeh.WhispersOfRlyeh;
import com.whispersofrlyeh.space.Planet;
import com.whispersofrlyeh.space.PlanetChunkGenerator;
import com.whispersofrlyeh.space.SpaceChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * As dimensões são definidas por JSON em data/whispersofrlyeh/dimension/.
 * Aqui ficam as chaves e os geradores de chunks (Espaço e planetas).
 */
public final class ModDimensions {
    public static final ResourceKey<Level> SPACE = ResourceKey.create(Registries.DIMENSION, WhispersOfRlyeh.id("space"));

    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, WhispersOfRlyeh.MODID);

    static {
        CHUNK_GENERATORS.register("space", () -> SpaceChunkGenerator.CODEC);
        CHUNK_GENERATORS.register("planet", () -> PlanetChunkGenerator.CODEC);
    }

    public static boolean isSpace(Level level) {
        return level.dimension() == SPACE || level.dimension().equals(SPACE);
    }

    /** Planeta desta dimensão, ou null. */
    public static Planet planetOf(Level level) {
        return Planet.byDimension(level.dimension());
    }

    /** Onde o "jetpack" do traje funciona: Espaço e gigantes gasosos. */
    public static boolean hasFreeFlight(Level level) {
        if (isSpace(level)) return true;
        Planet p = planetOf(level);
        return p != null && p.isGas();
    }

    private ModDimensions() {}
}
