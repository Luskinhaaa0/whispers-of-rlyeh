package com.whispersofrlyeh.entity;

import com.whispersofrlyeh.sanity.SanityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Deep One: humanoide anfíbio servo de Dagon, com modelo 3D animado (GeckoLib).
 * Usa a IA do Afogado (nada, caça no mar à noite), mas luta com garras em vez de tridente,
 * é mais forte e cada golpe arranha a mente: -3 de sanidade.
 * Só de ser visto já drena sanidade (veja SanityEvents).
 */
public class DeepOne extends Drowned implements GeoEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.deep_one.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.deep_one.walk");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.deep_one.swim");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.deep_one.attack");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public DeepOne(EntityType<? extends Drowned> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 50.0) // metade de um Golem de Ferro
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    // ---------- Animações ----------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (state.isMoving()) {
                return state.setAndContinue(this.isInWater() ? SWIM : WALK);
            }
            return state.setAndContinue(IDLE);
        }));

        controllers.add(new AnimationController<>(this, "attack", 2, state -> {
            if (this.swinging) {
                return state.setAndContinue(ATTACK);
            }
            state.resetCurrentAnimation();
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /** Duração do golpe, para a animação de ataque (0,5 s) tocar inteira. */
    public int getCurrentSwingDuration() {
        return 10;
    }

    // ---------- Comportamento ----------

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof ServerPlayer player) {
            SanityManager.add(player, -3);
        }
        return hit;
    }

    /** Deep Ones lutam com garras: nada de tridente nas mãos. */
    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
    }

    /** Não pegam itens do chão (o modelo 3D não mostra itens nas mãos). */
    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    /** Não monta em nada (nada de Deep One jóquei de galinha). */
    @Override
    public boolean startRiding(Entity vehicle, boolean force) {
        return false;
    }

    /** Respira debaixo d'água (é um ser do mar, afinal). */
    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    /** Não queima no sol como zumbis. */
    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    /** Sem filhotes. */
    @Override
    public void setBaby(boolean baby) {
        super.setBaby(false);
    }

    /** Nasce na água, no escuro, fora do Pacífico. */
    public static boolean checkSpawnRules(EntityType<DeepOne> type, ServerLevelAccessor level, MobSpawnType reason,
                                          BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL
                && level.getFluidState(pos).is(FluidTags.WATER)
                && Monster.isDarkEnoughToSpawn(level, pos, random);
    }
}
