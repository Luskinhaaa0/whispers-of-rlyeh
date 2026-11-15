package com.whispersofrlyeh.entity;

import com.whispersofrlyeh.sanity.SanityManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Shoggoth: massa de protoplasma negro que se arrasta, cheia de olhos, com uma bocarra,
 * 4 tentáculos longos e calmos e 16 tentáculos pequenos e inquietos.
 * Criado pelos Antigos como servo, rebelou-se. "Tekeli-li! Tekeli-li!"
 *
 *  - 150 de vida, 12 de dano, imune a empurrão e a queda, respira na água
 *  - Projéteis causam metade do dano (flechas afundam na gosma)
 *  - Regenera 1 de vida por segundo e se cura ao acertar golpes
 *  - Golpe: cegueira + lentidão II por 3 s e -6 de sanidade
 *  - Grito "Tekeli-li": -6 de sanidade de todos os jogadores num raio de 24 blocos
 *  - Quando está bloqueado, devora blocos moles à frente (folhas, vidro, terra, lã...)
 *    se a regra mobGriefing estiver ligada
 *  - Caça jogadores, aldeões, golens e animais
 */
public class Shoggoth extends Monster implements GeoEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.shoggoth.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.shoggoth.walk");
    private static final RawAnimation TENTACLES = RawAnimation.begin().thenLoop("animation.shoggoth.tentacles");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.shoggoth.attack");
    private static final RawAnimation ROAR = RawAnimation.begin().thenPlay("animation.shoggoth.roar");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int roarCooldown = 100;

    public Shoggoth(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 150.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Animal.class, false));
    }

    // ---------- Animações ----------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 6, state -> {
            if (state.isMoving()) {
                return state.setAndContinue(WALK);
            }
            return state.setAndContinue(IDLE);
        }));

        // Tentáculos sempre vivos: os calmos ondulam devagar, os inquietos se contorcem sem parar
        controllers.add(new AnimationController<>(this, "tentacles", 0, state -> state.setAndContinue(TENTACLES)));

        controllers.add(new AnimationController<>(this, "attack", 2, state -> {
            if (this.swinging) {
                return state.setAndContinue(ATTACK);
            }
            state.resetCurrentAnimation();
            return PlayState.STOP;
        }));

        controllers.add(new AnimationController<>(this, "roar", 2, state -> PlayState.STOP)
                .triggerableAnim("roar", ROAR));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /** Duração do golpe, para a animação de ataque (0,6 s) tocar inteira. */
    public int getCurrentSwingDuration() {
        return 12;
    }

    // ---------- Comportamento ----------

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) return;

        // Regeneração
        if (this.tickCount % 20 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(1.0f);
        }

        // Tekeli-li!
        if (this.roarCooldown > 0) {
            this.roarCooldown--;
        } else if (this.getTarget() != null && this.random.nextInt(80) == 0) {
            roar();
        }

        // Devora blocos moles quando está bloqueado
        if (this.horizontalCollision && this.tickCount % 10 == 0
                && this.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            devourSoftBlocks();
        }
    }

    private void roar() {
        this.roarCooldown = 240;
        this.triggerAnim("roar", "roar");
        this.playSound(SoundEvents.WARDEN_ROAR, 2.5f, 0.55f);

        for (ServerPlayer player : this.level().getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(24))) {
            if (player.isCreative() || player.isSpectator()) continue;
            SanityManager.add(player, -6);
            player.displayClientMessage(Component.translatable("message.whispersofrlyeh.tekeli_li")
                    .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD), true);
        }
    }

    private void devourSoftBlocks() {
        AABB box = this.getBoundingBox().inflate(0.4, 0.0, 0.4);
        int feetY = this.blockPosition().getY();
        for (BlockPos pos : BlockPos.betweenClosed(
                Mth.floor(box.minX), feetY, Mth.floor(box.minZ),
                Mth.floor(box.maxX), Mth.floor(box.maxY), Mth.floor(box.maxZ))) {
            BlockState state = this.level().getBlockState(pos);
            if (state.isAir() || state.is(BlockTags.WITHER_IMMUNE)) continue;
            float hardness = state.getDestroySpeed(this.level(), pos);
            if (hardness >= 0 && hardness <= 1.0f) {
                this.level().destroyBlock(pos, true, this);
                this.heal(0.5f);
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            this.heal(4.0f);
            if (target instanceof ServerPlayer player) {
                SanityManager.add(player, -6);
            }
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            amount *= 0.5f; // flechas afundam na gosma
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean startRiding(Entity vehicle, boolean force) {
        return false;
    }

    // ---------- Sons ----------

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SLIME_SQUISH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.SLIME_SQUISH, 0.6f, 0.5f);
    }

    @Override
    public float getVoicePitch() {
        return 0.45f + this.random.nextFloat() * 0.1f;
    }

    @Override
    protected float getSoundVolume() {
        return 1.6f;
    }

    // ---------- Spawn ----------

    /** Só nas profundezas (abaixo de Y 0), no escuro. */
    public static boolean checkSpawnRules(EntityType<Shoggoth> type, ServerLevelAccessor level, MobSpawnType reason,
                                          BlockPos pos, RandomSource random) {
        return pos.getY() < 0 && Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }
}
