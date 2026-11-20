package com.whispersofrlyeh.client;

import com.whispersofrlyeh.network.SpaceTransitionPayload;
import com.whispersofrlyeh.registry.ModDimensions;
import com.whispersofrlyeh.space.Planet;
import com.whispersofrlyeh.space.ClientSpaceState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Efeitos de tela das viagens espaciais:
 *  - Subindo no Overworld (acima de Y 320) o céu vai escurecendo até o preto do espaço,
 *    com uma faixa azul de atmosfera embaixo e estrelas aparecendo.
 *  - Saindo da Terra: o preto se abre revelando o Espaço (com a Terra lá embaixo).
 *  - Reentrada (Terra ou planeta): bordas em chamas e clarão.
 *  - Saindo de um planeta: escurece suavemente.
 */
public final class SpaceTransitionOverlay {

    private static final long DURATION = 3000;
    private static final int[][] STARS = makeStars();

    private static int[][] makeStars() {
        RandomSource r = RandomSource.create(77L);
        int[][] s = new int[220][3];
        for (int[] star : s) {
            star[0] = r.nextInt(10000);
            star[1] = r.nextInt(10000);
            star[2] = 120 + r.nextInt(135);
        }
        return s;
    }

    /** Camada da HUD (fica por baixo da HUD normal). */
    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        int w = g.guiWidth(), h = g.guiHeight();

        // Céu escurecendo durante a subida
        if (mc.level.dimension() == Level.OVERWORLD) {
            double y = mc.player.getY();
            if (y > 320) {
                float t = (float) Mth.clamp((y - 320) / 280.0, 0, 1);
                drawAscent(g, w, h, t);
            }
        }
        if (ModDimensions.isSpace(mc.level)) {
            drawApproach(g, w, h, mc);
        }
        drawTransition(g, w, h);
    }

    /**
     * Chegando perto de um planeta no Espaço: a tela vai sendo tomada pela cor da atmosfera dele,
     * com riscos de velocidade, até atravessar e trocar de dimensão (como um portal).
     */
    private static void drawApproach(GuiGraphics g, int w, int h, Minecraft mc) {
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        Planet nearest = null;
        double best = Double.MAX_VALUE;
        for (Planet p : Planet.values()) {
            double q = p.distanceInSpace(px, py, pz) / p.radius;
            if (q < best) { best = q; nearest = p; }
        }
        if (nearest == null || best > 1.7) return;

        float t = (float) Mth.clamp((1.7 - best) / 0.7, 0, 1); // 0 longe -> 1 entrando
        float[] c = PlanetSphereRenderer.styleOf(nearest).atmosphere();
        if (c == null) c = new float[]{0.75f, 0.75f, 0.75f};
        int r = (int) (c[0] * 255), gr = (int) (c[1] * 255), b = (int) (c[2] * 255);

        float pulse = 0.85f + 0.15f * (float) Math.sin(System.currentTimeMillis() * 0.012);
        int band = (int) (h * (0.15f + 0.35f * t));
        g.fillGradient(0, 0, w, band, argb(t * 0.75f * pulse, r, gr, b), argb(0, r, gr, b));
        g.fillGradient(0, h - band, w, h, argb(0, r, gr, b), argb(t * 0.75f * pulse, r, gr, b));
        g.fill(0, 0, w, h, argb(t * t * 0.55f, r, gr, b));

        // riscos de velocidade saindo do centro
        long seed = System.currentTimeMillis() / 50;
        RandomSource rand = RandomSource.create(seed);
        int streaks = (int) (40 * t);
        for (int i = 0; i < streaks; i++) {
            double ang = rand.nextDouble() * Math.PI * 2;
            double r0 = (0.25 + rand.nextDouble() * 0.5) * Math.max(w, h) * 0.5;
            double len = 8 + rand.nextDouble() * 30 * t;
            int x0 = (int) (w / 2 + Math.cos(ang) * r0), y0 = (int) (h / 2 + Math.sin(ang) * r0);
            int x1 = (int) (w / 2 + Math.cos(ang) * (r0 + len)), y1 = (int) (h / 2 + Math.sin(ang) * (r0 + len));
            g.fill(Math.min(x0, x1), Math.min(y0, y1), Math.max(x0, x1) + 1, Math.max(y0, y1) + 1,
                    argb(t * 0.5f, 255, 255, 255));
        }

        if (mc.player.tickCount % 20 == 0) {
            mc.gui.setOverlayMessage(Component.translatable("message.whispersofrlyeh.approach",
                    Component.translatable("planet.whispersofrlyeh." + nearest.id)).withStyle(ChatFormatting.GOLD), false);
        }
    }

    /** Usado também pela tela de transição enquanto o novo mundo carrega. */
    public static void drawTransition(GuiGraphics g, int w, int h) {
        int type = ClientSpaceState.transitionType;
        if (type < 0) return;
        long elapsed = System.currentTimeMillis() - ClientSpaceState.transitionStart;
        if (elapsed > DURATION) {
            ClientSpaceState.transitionType = -1;
            return;
        }
        float t = elapsed / (float) DURATION; // 0 -> 1

        switch (type) {
            case SpaceTransitionPayload.LEAVE_EARTH -> {
                // começa preto com estrelas e a atmosfera azul ficando para trás (desce pela tela)
                float a = 1f - smooth(t);
                g.fill(0, 0, w, h, argb(a * 0.95f, 0, 0, 0));
                stars(g, w, h, a);
                int glowTop = (int) (h * (0.55f + t * 0.6f));
                g.fillGradient(0, glowTop, w, h + (int) (h * 0.3f), argb(0, 60, 140, 255), argb(a * 0.8f, 120, 190, 255));
            }
            case SpaceTransitionPayload.ENTER_EARTH, SpaceTransitionPayload.ENTER_PLANET -> {
                // reentrada: clarão branco-alaranjado + bordas em chamas pulsando
                float flash = Math.max(0f, 1f - t * 2.2f);
                g.fill(0, 0, w, h, argb(flash * 0.85f, 255, 220, 170));
                float fire = (1f - t) * (0.6f + 0.4f * (float) Math.sin(elapsed * 0.03));
                int band = (int) (h * 0.28f);
                g.fillGradient(0, 0, w, band, argb(fire * 0.8f, 255, 110, 20), argb(0, 255, 60, 0));
                g.fillGradient(0, h - band, w, h, argb(0, 255, 60, 0), argb(fire * 0.8f, 255, 110, 20));
                g.fillGradient(0, 0, w, h, argb(fire * 0.18f, 255, 80, 0), argb(fire * 0.18f, 255, 80, 0));
            }
            case SpaceTransitionPayload.LEAVE_PLANET -> {
                float a = 1f - smooth(t);
                g.fill(0, 0, w, h, argb(a * 0.9f, 0, 0, 0));
                stars(g, w, h, a);
            }
            default -> { }
        }
    }

    private static void drawAscent(GuiGraphics g, int w, int h, float t) {
        // topo: preto do espaço | base: brilho azul da atmosfera
        g.fillGradient(0, 0, w, h, argb(t * 0.88f, 2, 4, 12), argb(t * 0.35f, 40, 90, 200));
        stars(g, w, h * 2 / 3, t * t);
    }

    private static void stars(GuiGraphics g, int w, int h, float alpha) {
        if (alpha <= 0.02f) return;
        for (int[] s : STARS) {
            int x = s[0] * w / 10000, y = s[1] * h / 10000;
            g.fill(x, y, x + 1, y + 1, argb(alpha * s[2] / 255f, 255, 255, 255));
        }
    }

    private static float smooth(float t) {
        return t * t * (3 - 2 * t);
    }

    private static int argb(float a, int r, int g, int b) {
        int ai = Mth.clamp((int) (a * 255), 0, 255);
        return (ai << 24) | (r << 16) | (g << 8) | b;
    }

    private SpaceTransitionOverlay() {}
}
