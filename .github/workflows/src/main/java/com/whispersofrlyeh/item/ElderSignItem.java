package com.whispersofrlyeh.item;

import com.whispersofrlyeh.sanity.SanityManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * O Elder Sign (Sinal Ancião).
 *  - Passivo: no inventário, reduz pela metade a perda de sanidade.
 *  - Ativo: clique direito para repelir monstros num raio de 8 blocos
 *    (gasta 1 de durabilidade, recupera 5 de sanidade, recarga de 30 s).
 */
public class ElderSignItem extends Item {
    private static final int COOLDOWN_TICKS = 600;
    private static final double RADIUS = 8.0;

    public ElderSignItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            List<Monster> monsters = level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(RADIUS));
            for (Monster monster : monsters) {
                // empurra para longe do jogador
                monster.knockback(2.0, player.getX() - monster.getX(), player.getZ() - monster.getZ());
                monster.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            }

            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.4f);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        40, 1.5, 0.5, 1.5, 0.05);
            }

            if (player instanceof ServerPlayer serverPlayer) {
                SanityManager.add(serverPlayer, 5);
                serverPlayer.displayClientMessage(
                        Component.translatable("message.whispersofrlyeh.elder_sign.repel")
                                .withStyle(ChatFormatting.GOLD),
                        true);
            }

            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.whispersofrlyeh.elder_sign.tooltip")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.whispersofrlyeh.elder_sign.tooltip.use")
                .withStyle(ChatFormatting.GOLD));
    }
}
