package com.whispersofrlyeh.client;

import com.whispersofrlyeh.WhispersOfRlyeh;
import com.whispersofrlyeh.registry.ModEntities;
import com.whispersofrlyeh.sanity.ClientSanityData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Alucinações que só o próprio jogador vê (nada disso existe no servidor):
 *  - Uma figura (Deep One, Enderman ou Shoggoth) parada ao longe, atrás de você, encarando. Some quando você se
 *    aproxima, quando olha fixo para ela, ou depois de alguns segundos.
 *  - Dor fantasma: a tela treme e você ouve o som de dano, sem perder vida.
 */
@EventBusSubscriber(modid = WhispersOfRlyeh.MODID, value = Dist.CLIENT)
public final class Hallucinations {

    private static Entity phantom;
    private static int phantomAge;
    private static int nextFakeId = -100000;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;

        if (player == null || level == null) {
            phantom = null;
            return;
        }
        if (mc.isPaused()) return;

        if (phantom != null && phantom.level() != level) {
            phantom = null; // trocou de mundo
        }
        if (player.isCreative() || player.isSpectator()) {
            removePhantom(level, false);
            return;
        }

        int sanity = ClientSanityData.get();
        RandomSource random = player.getRandom();

        if (phantom != null) {
            tickPhantom(level, player, random);
        } else if (sanity < 35 && random.nextInt(sanity < 15 ? 500 : 1400) == 0) {
            spawnPhantom(level, player, random);
        }

        // Dor fantasma
        if (sanity < 20 && random.nextInt(sanity == 0 ? 400 : 900) == 0) {
            player.animateHurt(0);
            level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0f, 1.0f, false);
        }
    }

    private static void spawnPhantom(ClientLevel level, LocalPlayer player, RandomSource random) {
        // Atrás do jogador, entre 10 e 16 blocos
        float angle = (player.getYRot() + 180f + (random.nextFloat() - 0.5f) * 120f) * Mth.DEG_TO_RAD;
        double dist = 10 + random.nextDouble() * 6;
        int x = Mth.floor(player.getX() - Mth.sin(angle) * dist);
        int z = Mth.floor(player.getZ() + Mth.cos(angle) * dist);

        BlockPos ground = null;
        for (int y = player.getBlockY() + 4; y >= player.getBlockY() - 8; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockPos below = pos.below();
            if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                    && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                    && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
                ground = pos;
                break;
            }
        }
        if (ground == null) return;

        int pick = random.nextInt(5);
        EntityType<?> type = pick == 0 ? EntityType.ENDERMAN
                : pick == 1 ? ModEntities.SHOGGOTH.get()
                : ModEntities.DEEP_ONE.get();
        Entity entity = type.create(level);
        if (entity == null) return;

        entity.setId(nextFakeId--);
        entity.setSilent(true);
        if (entity instanceof Mob mob) mob.setNoAi(true);
        entity.moveTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, 0f, 0f);
        level.addEntity(entity);

        phantom = entity;
        phantomAge = 0;
        facePlayer(player);
    }

    private static void tickPhantom(ClientLevel level, LocalPlayer player, RandomSource random) {
        phantomAge++;
        facePlayer(player);

        Vec3 look = player.getViewVector(1.0f);
        Vec3 toPhantom = phantom.position().add(0, phantom.getBbHeight() * 0.8, 0)
                .subtract(player.getEyePosition()).normalize();
        boolean staredAt = look.dot(toPhantom) > 0.96;

        boolean vanish = phantom.distanceTo(player) < 7
                || phantomAge > 200
                || (staredAt && phantomAge > 10 && random.nextInt(12) == 0);
        if (vanish) {
            removePhantom(level, true);
        }
    }

    private static void facePlayer(LocalPlayer player) {
        double dx = player.getX() - phantom.getX();
        double dz = player.getZ() - phantom.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
        phantom.setYRot(yaw);
        phantom.yRotO = yaw;
        phantom.setYHeadRot(yaw);
        if (phantom instanceof LivingEntity living) {
            living.yHeadRotO = yaw;
            living.yBodyRot = yaw;
            living.yBodyRotO = yaw;
        }
    }

    private static void removePhantom(ClientLevel level, boolean withEffect) {
        if (phantom == null) return;
        if (withEffect) {
            for (int i = 0; i < 12; i++) {
                level.addParticle(ParticleTypes.LARGE_SMOKE,
                        phantom.getX() + (level.random.nextDouble() - 0.5),
                        phantom.getY() + level.random.nextDouble() * 2,
                        phantom.getZ() + (level.random.nextDouble() - 0.5),
                        0, 0.02, 0);
            }
            level.playLocalSound(phantom.getX(), phantom.getY(), phantom.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.4f, 0.5f, false);
        }
        level.removeEntity(phantom.getId(), Entity.RemovalReason.DISCARDED);
        phantom = null;
    }

    private Hallucinations() {}
}
