package com.whispersofrlyeh.entity;

import com.whispersofrlyeh.sanity.SanityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Deep One: humanoide anfíbio servo de Dagon.
 * Usa a IA do Afogado (nada, caça no mar à noite, pode vir com tridente),
 * mas é mais forte e cada golpe arranha a mente: -3 de sanidade.
 * Só de ser visto já drena sanidade (veja SanityEvents).
 */
public class DeepOne extends Drowned {

    public DeepOne(EntityType<? extends Drowned> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof ServerPlayer player) {
            SanityManager.add(player, -3);
        }
        return hit;
    }

    /** Nasce na água, no escuro, fora do Pacífico. */
    public static boolean checkSpawnRules(EntityType<DeepOne> type, ServerLevelAccessor level, MobSpawnType reason,
                                          BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL
                && level.getFluidState(pos).is(FluidTags.WATER)
                && Monster.isDarkEnoughToSpawn(level, pos, random);
    }
}
