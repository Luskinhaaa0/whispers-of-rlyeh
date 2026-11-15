package com.whispersofrlyeh.client;

import com.whispersofrlyeh.WhispersOfRlyeh;
import com.whispersofrlyeh.command.SpeedCommand;
import com.whispersofrlyeh.registry.ModDimensions;
import com.whispersofrlyeh.space.ClientSpaceState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Lado do cliente do Espaço:
 *  - pula a tela "Carregando terreno" nas transições atmosféricas
 *  - tira a névoa (para os planetas distantes não sumirem no preto)
 *  - controle livre no ar: ESPAÇO sobe, SHIFT desce, WASD empurra para qualquer direção
 */
@EventBusSubscriber(modid = WhispersOfRlyeh.MODID, value = Dist.CLIENT)
public final class SpaceClientEvents {

    private static final double THRUST = 0.025;
    private static final double VERTICAL_THRUST = 0.035;
    private static final double MAX_SPEED = 1.4;

    private static boolean inSpace() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && ModDimensions.isSpace(mc.level);
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof ReceivingLevelScreen
                && System.currentTimeMillis() < ClientSpaceState.skipLoadingScreenUntil) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!inSpace()) return;
        event.setNearPlaneDistance(4096f);
        event.setFarPlaneDistance(8192f);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!inSpace()) return;
        event.setRed(0f);
        event.setGreen(0f);
        event.setBlue(0f);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.isPaused() || !inSpace()) return;
        if (player.onGround() || player.isFallFlying() || player.getAbilities().flying || player.isPassenger()) return;

        float forward = player.input.forwardImpulse;
        float strafe = player.input.leftImpulse;
        double up = (player.input.jumping ? VERTICAL_THRUST : 0) - (player.input.shiftKeyDown ? VERTICAL_THRUST : 0);

        // O /speed também acelera o impulso no Espaço
        double mult = SpeedCommand.getMultiplier(player);
        up *= mult;

        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yaw), cos = Mth.cos(yaw);
        double ax = (strafe * cos - forward * sin) * THRUST * mult;
        double az = (forward * cos + strafe * sin) * THRUST * mult;

        Vec3 v = player.getDeltaMovement().add(ax, up, az);
        double max = MAX_SPEED * Math.max(1.0, mult);
        if (v.length() > max) v = v.normalize().scale(max);
        player.setDeltaMovement(v);
    }

    private SpaceClientEvents() {}
}
