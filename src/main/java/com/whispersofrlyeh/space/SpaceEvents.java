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
 * Regras do Espaço que rodam no servidor:
 *  - Transição atmosférica: Overworld Y > 600  ->  Espaço Y 100 (mesmo X/Z, mesma velocidade)
 *                           Espaço Y < 0       ->  Overworld Y 580 (com Queda Lenta até pousar)
 *  - Gravidade reduzida em 85% no Espaço
 *  - Dano de queda bem menor no Espaço
 *  - Chegar perto do Sol queima
 */
@EventBusSubscriber(modid = WhispersOfRlyeh.MODID)
public final class SpaceEvents {

    public static final double OVERWORLD_EXIT_Y = 600;
    public static final double SPACE_ENTRY_Y = 100;
    public static final double SPACE_EXIT_Y = 0;
    public static final double OVERWORLD_ENTRY_Y = 580;

    private static final ResourceLocation LOW_GRAVITY = WhispersOfRlyeh.id("space_low_gravity");
    private static final String RETURNING_TAG = "whispersofrlyeh_returning";

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        boolean inSpace = ModDimensions.isSpace(level);

        updateGravity(player, inSpace);

        if (level.dimension() == Level.OVERWORLD && player.getY() > OVERWORLD_EXIT_Y) {
            transfer(player, ModDimensions.SPACE, SPACE_ENTRY_Y, false);
            player.displayClientMessage(Component.translatable("message.whispersofrlyeh.enter_space")
                    .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC), true);
            return;
        }
        if (inSpace && player.getY() < SPACE_EXIT_Y) {
            transfer(player, Level.OVERWORLD, OVERWORLD_ENTRY_Y, true);
            player.displayClientMessage(Component.translatable("message.whispersofrlyeh.return_earth")
                    .withStyle(ChatFormatting.AQUA), true);
            return;
        }

        // Queda Lenta de reentrada: dura até tocar o chão
        if (player.getPersistentData().getBoolean(RETURNING_TAG) && player.onGround()) {
            player.getPersistentData().remove(RETURNING_TAG);
            player.removeEffect(MobEffects.SLOW_FALLING);
        }

        if (inSpace && player.tickCount % 20 == 0) {
            sunHeat(player);
        }
    }

    /** Teleporta mantendo X/Z, rotação e velocidade, sem tela de carregamento no cliente. */
    private static void transfer(ServerPlayer player, ResourceKey<Level> destination, double y, boolean reentry) {
        ServerLevel target = player.server.getLevel(destination);
        if (target == null) return;

        Vec3 velocity = player.getDeltaMovement();
        boolean wasFlying = player.isFallFlying();

        PacketDistributor.sendToPlayer(player, new SpaceTransitionPayload());
        player.teleportTo(target, player.getX(), y, player.getZ(), player.getYRot(), player.getXRot());

        player.setDeltaMovement(velocity);
        player.hurtMarked = true;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        if (wasFlying) player.startFallFlying();

        if (reentry) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 60 * 5, 0, false, false, true));
            player.getPersistentData().putBoolean(RETURNING_TAG, true);
            player.fallDistance = 0;
        }
    }

    private static void updateGravity(ServerPlayer player, boolean inSpace) {
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity == null) return;
        boolean has = gravity.hasModifier(LOW_GRAVITY);
        if (inSpace && !has) {
            gravity.addTransientModifier(new AttributeModifier(LOW_GRAVITY, -0.85, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!inSpace && has) {
            gravity.removeModifier(LOW_GRAVITY);
        }
    }

    private static void sunHeat(ServerPlayer player) {
        SpaceBodies.Body sun = SpaceBodies.SUN;
        double d = sun.distanceTo(player.getX(), player.getY(), player.getZ());
        if (d < sun.radius() + 30 && !player.isCreative() && !player.isSpectator()) {
            player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), 80));
            player.displayClientMessage(Component.translatable("message.whispersofrlyeh.sun_heat")
                    .withStyle(ChatFormatting.GOLD), true);
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (ModDimensions.isSpace(event.getEntity().level())) {
            event.setDamageMultiplier(event.getDamageMultiplier() * 0.15f);
        }
    }

    private SpaceEvents() {}
}
