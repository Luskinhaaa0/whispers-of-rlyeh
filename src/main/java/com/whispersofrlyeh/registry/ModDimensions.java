package com.whispersofrlyeh.registry;

import com.mojang.serialization.MapCodec;
import com.whispersofrlyeh.WhispersOfRlyeh;
import com.whispersofrlyeh.space.SpaceChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * A dimensão do Espaço é definida por JSON em data/whispersofrlyeh/dimension/space.json.
 * Aqui registramos só o gerador de chunks que constrói os planetas.
 */
public final class ModDimensions {
    public static final ResourceKey<Level> SPACE = ResourceKey.create(Registries.DIMENSION, WhispersOfRlyeh.id("space"));

    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, WhispersOfRlyeh.MODID);

    static {
        CHUNK_GENERATORS.register("space", () -> SpaceChunkGenerator.CODEC);
    }

    public static boolean isSpace(Level level) {
        return level.dimension() == SPACE;
    }

    private ModDimensions() {}
}
