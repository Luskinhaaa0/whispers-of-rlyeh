package com.whispersofrlyeh.space;

import com.whispersofrlyeh.WhispersOfRlyeh;
import com.whispersofrlyeh.network.SpaceTransitionPayload;
import com.whispersofrlyeh.registry.ModDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Viagens e perigos (lado do servidor):
 *
 *  Terra (Overworld) Y > 600      ->  Espaço Y 100 (mesmo X/Z e velocidade)
 *  Espaço Y < 0                   ->  Terra Y 580 com Queda Lenta até pousar
 *  Espaço: voar para DENTRO da esfera de um planeta  ->  dimensão do planeta (no alto)
 *  Planeta: subir acima do teto    ->  Espaço, ao lado do planeta
 *
 *  Gravidade: cada lugar tem a sua (Espaço 15%, Lua 17%, Marte 38%, Júpiter 160%...)
 *  Perigos: Sol queima | Vênus queima | gigantes gasosos: pressão, raios e esmagamento |
 *           Urano/Netuno congelam (armadura de couro protege)
 */
@EventBusSubscriber(modid = WhispersOfRlyeh.MODID)
public final class SpaceEvents {

    public static final double OVERWORLD_EXIT_Y = 600;
    public static final double SPACE_ENTRY_Y = 100;
    public static final double SPACE_EXIT_Y = 0;
    public static final double OVERWORLD_ENTRY_Y = 580;
    public static final double SPACE_GRAVITY = 0.15;

    private static final ResourceLocation GRAVITY_ID = WhispersOfRlyeh.id("space_low_gravity");
    private static final String RETURNING_TAG = "whispersofrlyeh_returning";

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        boolean inSpace = ModDimensions.isSpace(level);
        Planet planet = Planet.byDimension(level.dimension());

        updateGravity(player, inSpace ? SPACE_GRAVITY : planet != null ? planet.gravity : 1.0);

        // ---------- Transições ----------
        if (level.dimension() == Level.OVERWORLD && player.getY() > OVERWORLD_EXIT_Y) {
            transfer(player, ModDimensions.SPACE, player.getX(), SPACE_ENTRY_Y, player.getZ(),
                    SpaceTransitionPayload.LEAVE_EARTH, false);
            message(player, Component.translatable("message.whispersofrlyeh.enter_space"), ChatFormatting.DARK_AQUA);
            return;
        }
        if (inSpace) {
            if (player.getY() < SPACE_EXIT_Y) {
                transfer(player, Level.OVERWORLD, player.getX(), OVERWORLD_ENTRY_Y, player.getZ(),
                        SpaceTransitionPayload.ENTER_EARTH, true);
                message(player, Component.translatable("message.whispersofrlyeh.return_earth"), ChatFormatting.AQUA);
                return;
            }
            for (Planet p : Planet.values()) {
                if (p.distanceInSpace(player.getX(), player.getY(), player.getZ()) < p.radius) {
                    enterPlanet(player, p);
                    return;
                }
            }
        } else if (planet != null && player.getY() > planet.exitY()) {
            leavePlanet(player, planet);
            return;
        }

        // Queda Lenta de reentrada: dura até tocar o chão
        if (player.getPersistentData().getBoolean(RETURNING_TAG) && player.onGround()) {
            player.getPersistentData().remove(RETURNING_TAG);
            player.removeEffect(MobEffects.SLOW_FALLING);
        }

        // ---------- Perigos (1x por segundo) ----------
        if (player.tickCount % 20 != 0 || player.isCreative() || player.isSpectator()) return;
        if (inSpace) sunHeat(player);
        if (planet != null) hazards(player, level, planet);
    }

    private static void enterPlanet(ServerPlayer player, Planet p) {
        ServerLevel target = player.server.getLevel(p.dimension());
        if (target == null) return;
        // Cada ponto da esfera leva a um lugar diferente do planeta
        double x = (player.getX() - p.cx) * 4;
        double z = (player.getZ() - p.cz) * 4;
        transfer(player, p.dimension(), x, p.entryY(), z, SpaceTransitionPayload.ENTER_PLANET, !p.isGas());
        message(player, Component.translatable("message.whispersofrlyeh.enter_planet",
                Component.translatable("planet.whispersofrlyeh." + p.id)), ChatFormatting.GOLD);
    }

    private static void leavePlanet(ServerPlayer player, Planet p) {
        double ox = player.getX() / 4, oz = player.getZ() / 4;
        double len = Math.sqrt(ox * ox + oz * oz);
        if (len < 1) { ox = 1; oz = 0; len = 1; }
        double x = p.cx + ox / len * (p.radius + 25);
        double z = p.cz + oz / len * (p.radius + 25);
        double y = Math.max(20, Math.min(420, p.cy));
        transfer(player, ModDimensions.SPACE, x, y, z, SpaceTransitionPayload.LEAVE_PLANET, false);
        player.setDeltaMovement(ox / len * 0.6, 0.1, oz / len * 0.6);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        message(player, Component.translatable("message.whispersofrlyeh.leave_planet",
                Component.translatable("planet.whispersofrlyeh." + p.id)), ChatFormatting.DARK_AQUA);
    }

    /** Teleporta mantendo rotação e velocidade; o cliente mostra a animação de transição. */
    private static void transfer(ServerPlayer player, ResourceKey<Level> destination, double x, double y, double z,
                                 int transitionType, boolean slowFallUntilLanding) {
        ServerLevel target = player.server.getLevel(destination);
        if (target == null) return;

        Vec3 velocity = player.getDeltaMovement();
        boolean wasFlying = player.isFallFlying();

        PacketDistributor.sendToPlayer(player, new SpaceTransitionPayload(transitionType));
        player.teleportTo(target, x, y, z, player.getYRot(), player.getXRot());

        player.setDeltaMovement(velocity);
        player.hurtMarked = true;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        if (wasFlying) player.startFallFlying();
        player.fallDistance = 0;

        if (slowFallUntilLanding) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 60 * 5, 0, false, false, true));
            player.getPersistentData().putBoolean(RETURNING_TAG, true);
        }
    }

    private static void updateGravity(ServerPlayer player, double gravity) {
        AttributeInstance attr = player.getAttribute(Attributes.GRAVITY);
        if (attr == null) return;
        double wanted = gravity - 1.0;
        AttributeModifier current = attr.getModifier(GRAVITY_ID);
        if (Math.abs(wanted) < 0.001) {
            if (current != null) attr.removeModifier(GRAVITY_ID);
            return;
        }
        if (current == null || Math.abs(current.amount() - wanted) > 0.001) {
            attr.removeModifier(GRAVITY_ID);
            attr.addTransientModifier(new AttributeModifier(GRAVITY_ID, wanted, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void sunHeat(ServerPlayer player) {
        double d = SpaceBodies.sunDistance(player.getX(), player.getY(), player.getZ());
        if (d < SpaceBodies.SUN_RADIUS + 800) {
            player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), 100));
            message(player, Component.translatable("message.whispersofrlyeh.sun_heat"), ChatFormatting.GOLD);
        }
        if (d < SpaceBodies.SUN_RADIUS) {
            player.hurt(player.damageSources().inFire(), 8.0f);
        }
    }

    private static void hazards(ServerPlayer player, ServerLevel level, Planet p) {
        switch (p) {
            case VENUS -> {
                if (!player.hasEffect(MobEffects.FIRE_RESISTANCE) && level.canSeeSky(player.blockPosition())) {
                    player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), 40));
                    if (player.getRandom().nextInt(4) == 0) {
                        message(player, Component.translatable("message.whispersofrlyeh.venus_heat"), ChatFormatting.GOLD);
                    }
                }
            }
            case JUPITER, SATURN, URANUS, NEPTUNE -> gasHazards(player, level, p);
            default -> { }
        }
    }

    private static void gasHazards(ServerPlayer player, ServerLevel level, Planet p) {
        double y = player.getY();

        // Pressão: quanto mais fundo, pior
        if (y < 140) {
            float dmg = (float) (1 + (140 - y) / 25);
            player.hurt(player.damageSources().magic(), dmg);
            if (y < 90) player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false));
            if (y < 50) player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
            message(player, Component.translatable("message.whispersofrlyeh.pressure"), ChatFormatting.RED);
        }
        if (y < 8) {
            player.hurt(player.damageSources().fellOutOfWorld(), 40.0f);
            message(player, Component.translatable("message.whispersofrlyeh.crushed"), ChatFormatting.DARK_RED);
        }

        // Raios entre as nuvens
        if (player.getRandom().nextInt(p == Planet.JUPITER ? 6 : 12) == 0) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                double a = player.getRandom().nextDouble() * Math.PI * 2;
                double r = 12 + player.getRandom().nextDouble() * 30;
                bolt.moveTo(player.getX() + Math.cos(a) * r, y - 10 + player.getRandom().nextDouble() * 20,
                        player.getZ() + Math.sin(a) * r);
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
        }

        // Frio de Urano e Netuno (armadura de couro protege)
        if ((p == Planet.URANUS || p == Planet.NEPTUNE) && player.canFreeze()) {
            player.setTicksFrozen(player.getTicksRequiredToFreeze() + 30);
            if (player.getRandom().nextInt(5) == 0) {
                message(player, Component.translatable("message.whispersofrlyeh.freezing"), ChatFormatting.AQUA);
            }
        }
    }

    private static void message(ServerPlayer player, Component text, ChatFormatting color) {
        player.displayClientMessage(text.copy().withStyle(color), true);
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        Level level = event.getEntity().level();
        double g = 1.0;
        if (ModDimensions.isSpace(level)) g = SPACE_GRAVITY;
        else {
            Planet p = Planet.byDimension(level.dimension());
            if (p != null) g = p.gravity;
        }
        if (g < 1.0) event.setDamageMultiplier(event.getDamageMultiplier() * (float) g);
    }

    private SpaceEvents() {}
}
