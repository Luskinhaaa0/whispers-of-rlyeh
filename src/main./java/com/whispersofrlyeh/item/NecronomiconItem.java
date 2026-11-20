package com.whispersofrlyeh.item;

import com.whispersofrlyeh.sanity.SanityManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * O Necronomicon: clique com o botão direito para ler uma página.
 * Cada leitura lança um feitiço aleatório e custa sanidade.
 * Ler com a mente já frágil (sanidade < 15) machuca.
 */
public class NecronomiconItem extends Item {
    public static final int SANITY_COST = 12;
    private static final int COOLDOWN_TICKS = 200; // 10 segundos

    public NecronomiconItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            int sanityBefore = SanityManager.get(serverPlayer);

            castRandomSpell(serverPlayer);
            SanityManager.add(serverPlayer, -SANITY_COST);

            if (sanityBefore < 15) {
                serverPlayer.hurt(level.damageSources().magic(), 4.0f);
                serverPlayer.displayClientMessage(
                        Component.translatable("message.whispersofrlyeh.necronomicon.backlash")
                                .withStyle(ChatFormatting.DARK_RED),
                        false);
            }

            level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 0.5f);
            level.playSound(null, player.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.35f, 0.6f);
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static void castRandomSpell(ServerPlayer player) {
        int spell = player.getRandom().nextInt(4);

        switch (spell) {
            case 0 -> { // Visão Além do Véu: revela criaturas próximas
                List<LivingEntity> nearby = player.level().getEntitiesOfClass(
                        LivingEntity.class, player.getBoundingBox().inflate(48), other -> other != player);
                for (LivingEntity entity : nearby) {
                    entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0, false, false));
                }
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0));
            }
            case 1 -> { // Força dos Antigos
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0));
            }
            case 2 -> { // Bênção de Dagon
                player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 1200, 0));
                player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 1200, 0));
            }
            default -> // Conhecimento Proibido
                    player.giveExperiencePoints(40);
        }

        player.displayClientMessage(
                Component.translatable("message.whispersofrlyeh.necronomicon.spell." + spell)
                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC),
                true);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // brilho de encantamento
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.whispersofrlyeh.necronomicon.tooltip")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.whispersofrlyeh.necronomicon.tooltip.cost", SANITY_COST)
                .withStyle(ChatFormatting.DARK_RED));
    }
}
