package com.whispersofrlyeh.sanity;

import com.whispersofrlyeh.WhispersOfRlyeh;
import com.whispersofrlyeh.command.SanityCommand;
import com.whispersofrlyeh.entity.DeepOne;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Toda a lógica de sanidade que roda no servidor.
 *
 * PERDAS (a cada 3 segundos):
 *   Escuro (luz <= 3): -3  |  Penumbra (luz <= 7): -1
 *   Estar no End: -2       |  Submerso abaixo de Y 30: -2
 *   3+ monstros perto: -2  |  Ver um Deep One: -3
 *   Ídolo a até 6 blocos: -2 | Tempestade a céu aberto: -1
 *   Elder Sign no inventário: perdas pela metade
 *
 * RECUPERAÇÃO (mais lenta, a cada 6 segundos):
 *   Céu aberto de dia, sem chuva: +1
 *   Lugar bem iluminado (luz >= 12) com sanidade < 40: +1
 *   Elder Sign: +1 extra se já estiver recuperando
 *   Dormir a noite toda: +15 (ou sonhos de R'lyeh perto do ídolo)
 *   Chá Calmante: +15  |  Ler um livro comum: +6 (1 vez por minuto)
 */
@EventBusSubscriber(modid = WhispersOfRlyeh.MODID)
public final class SanityEvents {

    private static final int WHISPER_COUNT = 8;

    private static final SoundEvent[] WHISPER_SOUNDS = {
            SoundEvents.WARDEN_HEARTBEAT,
            SoundEvents.WARDEN_NEARBY_CLOSE,
            SoundEvents.ELDER_GUARDIAN_AMBIENT,
            SoundEvents.ENDERMAN_STARE,
            SoundEvents.SCULK_SHRIEKER_SHRIEK,
            SoundEvents.ZOMBIE_AMBIENT
    };

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (player.tickCount % 20 != 0) return; // uma vez por segundo

        if (player.tickCount % 60 == 0) applyDrain(player);
        if (player.tickCount % 120 == 0) applyRegen(player);
        applyMadnessEffects(player, SanityManager.get(player));
    }

    private static void applyDrain(ServerPlayer player) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        int light = level.getMaxLocalRawBrightness(pos);
        int drain = 0;

        if (light <= 3) drain += 3;
        else if (light <= 7) drain += 1;
        if (level.dimension() == Level.END) drain += 2;
        if (player.isUnderWater() && pos.getY() < 30) drain += 2;
        if (level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(10)).size() >= 3) drain += 2;
        if (seesDeepOne(player)) drain += 3;
        if (SanityManager.isNearIdol(player, 6)) drain += 2;
        if (level.isThundering() && level.canSeeSky(pos)) drain += 1;

        if (SanityManager.hasElderSign(player)) drain /= 2;
        if (drain > 0) SanityManager.add(player, -drain);
    }

    private static void applyRegen(ServerPlayer player) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        int regen = 0;

        if (level.isDay() && !level.isRaining() && level.canSeeSky(pos)) {
            regen += 1;
        } else if (level.getMaxLocalRawBrightness(pos) >= 12 && SanityManager.get(player) < 40) {
            regen += 1;
        }
        if (regen > 0 && SanityManager.hasElderSign(player)) regen += 1;
        if (regen > 0) SanityManager.add(player, regen);
    }

    private static boolean seesDeepOne(ServerPlayer player) {
        for (DeepOne deepOne : player.level().getEntitiesOfClass(DeepOne.class, player.getBoundingBox().inflate(16))) {
            if (player.hasLineOfSight(deepOne)) return true;
        }
        return false;
    }

    private static void applyMadnessEffects(ServerPlayer player, int sanity) {
        RandomSource random = player.getRandom();

        // < 70: sons que não deveriam estar ali
        if (sanity < 70 && random.nextFloat() < 0.05f) {
            SoundEvent sound = WHISPER_SOUNDS[random.nextInt(WHISPER_SOUNDS.length)];
            player.playNotifySound(sound, SoundSource.AMBIENT, 0.6f, 0.5f + random.nextFloat() * 0.3f);
        }

        // < 50: tontura
        if (sanity < 50 && random.nextFloat() < 0.04f) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 140, 0, false, false));
        }

        // < 30: escuridão, passos fantasmas, sussurros, fome de algo que não é comida
        if (sanity < 30) {
            if (random.nextFloat() < 0.04f) {
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, false, false));
            }
            if (random.nextFloat() < 0.06f) {
                player.playNotifySound(SoundEvents.STONE_STEP, SoundSource.HOSTILE, 0.6f, 0.8f);
            }
            if (random.nextFloat() < 0.05f) {
                player.displayClientMessage(
                        Component.translatable("message.whispersofrlyeh.whisper." + random.nextInt(WHISPER_COUNT))
                                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC),
                        true);
            }
            if (random.nextFloat() < 0.02f) {
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 0, false, false));
            }
        }

        // Zero: a loucura consome — fraqueza, lentidão e a mente sangra
        if (sanity == 0 && player.tickCount % 200 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 220, 1, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 220, 0, false, true));
            player.hurt(player.level().damageSources().magic(), 1.0f);
            player.displayClientMessage(
                    Component.translatable("message.whispersofrlyeh.madness").withStyle(ChatFormatting.DARK_RED),
                    true);
        }
    }

    @SubscribeEvent
    public static void onWakeUp(PlayerWakeUpEvent event) {
        // wakeImmediately == false quando o jogador dormiu a noite toda
        if (!(event.getEntity() instanceof ServerPlayer player) || event.wakeImmediately()) return;

        if (SanityManager.isNearIdol(player, 8)) {
            // Sonhos de R'lyeh: conhecimento em troca da mente
            SanityManager.add(player, -15);
            player.giveExperienceLevels(3);
            player.displayClientMessage(Component.translatable("message.whispersofrlyeh.idol_dream")
                    .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC), false);
        } else {
            SanityManager.add(player, 15);
        }
    }

    /** Ler um livro comum acalma a mente (1 vez por minuto). */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getItemStack().is(Items.BOOK)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.getCooldowns().isOnCooldown(Items.BOOK)) return;

        SanityManager.add(player, 6);
        player.getCooldowns().addCooldown(Items.BOOK, 1200);
        player.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 1.0f);
        player.displayClientMessage(Component.translatable("message.whispersofrlyeh.read_book")
                .withStyle(ChatFormatting.AQUA), true);
    }

    // Sincroniza a HUD ao entrar, renascer e trocar de dimensão
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SanityManager.sync(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SanityManager.sync(player);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SanityManager.sync(player);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        SanityCommand.register(event.getDispatcher());
    }

    private SanityEvents() {}
}
