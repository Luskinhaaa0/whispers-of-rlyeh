package com.whispersofrlyeh.sanity;

import com.whispersofrlyeh.WhispersOfRlyeh;
import com.whispersofrlyeh.command.SanityCommand;
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
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Toda a lógica de sanidade que roda no servidor.
 *
 * A cada 3 segundos:
 *   - Escuro (luz <= 3): -2
 *   - Estar no End: -1
 *   - Fundo do oceano (submerso abaixo de Y 30): -1
 *   - 3 ou mais monstros por perto: -1
 *   - Céu aberto durante o dia: +1
 *   - Lugar bem iluminado, sem nada drenando, com sanidade < 50: +1
 *   - Elder Sign no inventário: perdas pela metade e +1 de recuperação
 * Dormir a noite inteira: +30
 */
@EventBusSubscriber(modid = WhispersOfRlyeh.MODID)
public final class SanityEvents {

    private static final int WHISPER_COUNT = 6;

    private static final SoundEvent[] WHISPER_SOUNDS = {
            SoundEvents.WARDEN_HEARTBEAT,
            SoundEvents.WARDEN_NEARBY_CLOSE,
            SoundEvents.ELDER_GUARDIAN_AMBIENT,
            SoundEvents.ENDERMAN_STARE,
            SoundEvents.SCULK_SHRIEKER_SHRIEK
    };

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (player.tickCount % 20 != 0) return; // uma vez por segundo

        if (player.tickCount % 60 == 0) {
            updateSanity(player);
        }
        applyMadnessEffects(player, SanityManager.get(player));
    }

    private static void updateSanity(ServerPlayer player) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        int light = level.getMaxLocalRawBrightness(pos);

        int drain = 0;
        int regen = 0;

        if (light <= 3) drain += 2;
        if (level.dimension() == Level.END) drain += 1;
        if (player.isUnderWater() && pos.getY() < 30) drain += 1;
        if (level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(10)).size() >= 3) drain += 1;

        if (level.isDay() && level.canSeeSky(pos)) regen += 1;
        if (drain == 0 && light >= 10 && SanityManager.get(player) < 50) regen += 1;

        if (SanityManager.hasElderSign(player)) {
            drain /= 2;
            if (regen > 0) regen += 1;
        }

        int delta = regen - drain;
        if (delta != 0) {
            SanityManager.add(player, delta);
        }
    }

    private static void applyMadnessEffects(ServerPlayer player, int sanity) {
        RandomSource random = player.getRandom();

        // Abaixo de 60: sons que não deveriam estar ali
        if (sanity < 60 && random.nextFloat() < 0.04f) {
            SoundEvent sound = WHISPER_SOUNDS[random.nextInt(WHISPER_SOUNDS.length)];
            player.playNotifySound(sound, SoundSource.AMBIENT, 0.6f, 0.5f + random.nextFloat() * 0.3f);
        }

        // Abaixo de 40: tontura
        if (sanity < 40 && random.nextFloat() < 0.03f) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, false));
        }

        // Abaixo de 25: escuridão, passos fantasmas e sussurros
        if (sanity < 25) {
            if (random.nextFloat() < 0.03f) {
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false));
            }
            if (random.nextFloat() < 0.05f) {
                player.playNotifySound(SoundEvents.STONE_STEP, SoundSource.HOSTILE, 0.5f, 0.8f);
            }
            if (random.nextFloat() < 0.04f) {
                player.displayClientMessage(
                        Component.translatable("message.whispersofrlyeh.whisper." + random.nextInt(WHISPER_COUNT))
                                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC),
                        true);
            }
        }

        // Zero: a loucura consome o jogador
        if (sanity == 0 && player.tickCount % 200 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 220, 1, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 220, 0, false, true));
            player.displayClientMessage(
                    Component.translatable("message.whispersofrlyeh.madness").withStyle(ChatFormatting.DARK_RED),
                    true);
        }
    }

    @SubscribeEvent
    public static void onWakeUp(PlayerWakeUpEvent event) {
        // wakeImmediately == false quando o jogador dormiu a noite toda
        if (event.getEntity() instanceof ServerPlayer player && !event.wakeImmediately()) {
            SanityManager.add(player, 30);
        }
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
