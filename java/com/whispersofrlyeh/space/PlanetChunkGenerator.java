package com.whispersofrlyeh.space;

import com.mojang.serialization.Codec;
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
 * Gerador de terreno dos planetas. O campo "planet" no JSON da dimensão escolhe o planeta
 * (moon, mercury, mars, venus, jupiter, saturn, uranus, neptune) e o PlanetTerrain constrói o chunk.
 */
public class PlanetChunkGenerator extends FlatLevelSource {

    public static final MapCodec<PlanetChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            FlatLevelGeneratorSettings.CODEC.fieldOf("settings").forGetter(FlatLevelSource::settings),
            Codec.STRING.fieldOf("planet").forGetter(g -> g.planetId)
    ).apply(instance, PlanetChunkGenerator::new));

    private final String planetId;
    private final Planet planet;

    public PlanetChunkGenerator(FlatLevelGeneratorSettings settings, String planetId) {
        super(settings);
        this.planetId = planetId;
        this.planet = Planet.byId(planetId);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
        PlanetTerrain.generate(planet, chunk);
    }
}
