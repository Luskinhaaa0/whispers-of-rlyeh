package com.whispersofrlyeh.space;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

/**
 * Gerador do Espaço: um mundo plano SEM camadas (vazio total), onde cada chunk
 * recebe os blocos dos planetas que passam por ele (veja SpaceBodies).
 * Como cada chunk só calcula a sua fatia das esferas, planetas gigantes não pesam.
 */
public class SpaceChunkGenerator extends FlatLevelSource {

    public static final MapCodec<SpaceChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            FlatLevelGeneratorSettings.CODEC.fieldOf("settings").forGetter(FlatLevelSource::settings)
    ).apply(instance, SpaceChunkGenerator::new));

    public SpaceChunkGenerator(FlatLevelGeneratorSettings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
        SpaceBodies.generate(chunk);
    }
}
