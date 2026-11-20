package com.whispersofrlyeh.client;

import com.whispersofrlyeh.WhispersOfRlyeh;
import com.whispersofrlyeh.command.SpeedCommand;
import com.whispersofrlyeh.registry.ModDimensions;
import com.whispersofrlyeh.space.ClientSpaceState;
import com.whispersofrlyeh.space.Planet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Lado do cliente do Espaço e dos planetas:
 *  - troca a tela "Carregando terreno" pela animação de viagem
 *  - névoa: nenhuma no Espaço/Lua/Mercúrio; densa em Vênus e nos gigantes gasosos
 *  - propulsores (Espaço e gigantes gasosos): ESPAÇO sobe, SHIFT desce, WASD empurra
 *  - vento nos gigantes gasosos
 */
@EventBusSubscriber(modid = WhispersOfRlyeh.MODID, value = Dist.CLIENT)
public final class SpaceClientEvents {

    private static final double THRUST = 0.025;
    private static final double VERTICAL_THRUST = 0.035;
    private static final double MAX_SPEED = 1.4;

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof ReceivingLevelScreen receiving
                && System.currentTimeMillis() < ClientSpaceState.skipLoadingScreenUntil) {
            event.setNewScreen(new SpaceTransitionScreen(receiving));
        }
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        Planet planet = ModDimensions.planetOf(level);

        if (ModDimensions.isSpace(level) || planet == Planet.MOON || planet == Planet.MERCURY) {
            event.setNearPlaneDistance(4096f);
            event.setFarPlaneDistance(8192f);
            event.setCanceled(true);
        } else if (planet != null && planet.isGas()) {
            float far = planet == Planet.NEPTUNE ? 56f : planet == Planet.JUPITER ? 80f : 96f;
            event.setNearPlaneDistance(0f);
            event.setFarPlaneDistance(far);
            event.setCanceled(true);
        } else if (planet == Planet.VENUS) {
            event.setNearPlaneDistance(0f);
            event.setFarPlaneDistance(72f);
            event.setCanceled(true);
        } else if (planet == Planet.MARS) {
            event.setNearPlaneDistance(event.getFarPlaneDistance() * 0.4f);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        Planet planet = ModDimensions.planetOf(level);
        if (ModDimensions.isSpace(level) || planet == Planet.MOON || planet == Planet.MERCURY) {
            event.setRed(0f);
            event.setGreen(0f);
            event.setBlue(0f);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.isPaused()) return;
        if (!ModDimensions.hasFreeFlight(mc.level)) return;
        if (player.onGround() || player.isFallFlying() || player.getAbilities().flying || player.isPassenger()) return;

        Planet planet = ModDimensions.planetOf(mc.level);
        double mult = SpeedCommand.getMultiplier(player);

        // Nos gigantes gasosos o propulsor precisa vencer a gravidade forte
        double vertical = VERTICAL_THRUST;
        if (planet != null) vertical = 0.08 * planet.gravity * 1.35 + 0.02;

        float forward = player.input.forwardImpulse;
        float strafe = player.input.leftImpulse;
        double up = ((player.input.jumping ? vertical : 0) - (player.input.shiftKeyDown ? vertical : 0)) * mult;

        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yaw), cos = Mth.cos(yaw);
        double ax = (strafe * cos - forward * sin) * THRUST * mult;
        double az = (forward * cos + strafe * sin) * THRUST * mult;

        // Vento: faixas de vento em sentidos alternados conforme a altitude
        if (planet != null) {
            double strength = planet == Planet.NEPTUNE ? 0.035 : planet == Planet.JUPITER ? 0.025 : 0.015;
            ax += Math.sin(player.getY() * 0.05) * strength;
            az += Math.cos(player.getY() * 0.031 + player.tickCount * 0.01) * strength * 0.4;
        }

        Vec3 v = player.getDeltaMovement().add(ax, up, az);
        double max = MAX_SPEED * Math.max(1.0, mult);
        if (v.length() > max) v = v.normalize().scale(max);
        player.setDeltaMovement(v);
    }

    private SpaceClientEvents() {}
}
