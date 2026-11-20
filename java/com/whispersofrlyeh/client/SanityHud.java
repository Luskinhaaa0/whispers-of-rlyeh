package com.whispersofrlyeh.client;

import com.whispersofrlyeh.sanity.ClientSanityData;
import com.whispersofrlyeh.sanity.SanityManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Desenha:
 *  - uma barra vertical de sanidade à direita da hotbar
 *  - uma vinheta roxa pulsante nas bordas da tela quando a sanidade está baixa
 */
public final class SanityHud {

    private static final int BAR_HEIGHT = 36;
    private static final int BAR_WIDTH = 5;

    private static boolean shouldRender(Minecraft mc) {
        return mc.player != null
                && !mc.options.hideGui
                && !mc.player.isCreative()
                && !mc.player.isSpectator();
    }

    public static void renderBar(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!shouldRender(mc)) return;

        int sanity = ClientSanityData.get();
        int x = g.guiWidth() / 2 + 96;
        int bottom = g.guiHeight() - 3;

        // Com a mente frágil, a barra treme
        if (sanity < 25 && mc.player.getRandom().nextInt(6) == 0) {
            x += mc.player.getRandom().nextInt(3) - 1;
        }

        int filled = sanity * BAR_HEIGHT / SanityManager.MAX;

        g.fill(x - 1, bottom - BAR_HEIGHT - 1, x + BAR_WIDTH + 1, bottom + 1, 0xC0000000); // moldura
        g.fill(x, bottom - BAR_HEIGHT, x + BAR_WIDTH, bottom, 0xFF1A1420);                    // fundo
        g.fill(x, bottom - filled, x + BAR_WIDTH, bottom, colorFor(sanity));                  // preenchimento
    }

    public static void renderVignette(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!shouldRender(mc)) return;

        int sanity = ClientSanityData.get();
        if (sanity >= 40) return;

        float intensity = (40 - sanity) / 40f;
        float time = mc.player.tickCount + delta.getGameTimeDeltaPartialTick(false);
        float pulse = 0.5f + 0.5f * Mth.sin(time * 0.1f);
        int alpha = Mth.clamp((int) (intensity * (90 + 70 * pulse)), 0, 200);

        int w = g.guiWidth();
        int h = g.guiHeight();
        int band = h / 3;
        int rgb = 0x14001F; // roxo quase preto
        int edge = (alpha << 24) | rgb;
        int clear = rgb;    // alfa 0

        g.fillGradient(0, 0, w, band, edge, clear);       // topo
        g.fillGradient(0, h - band, w, h, clear, edge);   // base
        g.fill(0, 0, w, h, ((int) (alpha * 0.3f) << 24) | rgb);
    }

    private static int colorFor(int sanity) {
        if (sanity >= 60) return 0xFF3FB8AF; // verde-água: mente estável
        if (sanity >= 30) return 0xFF8A4FD8; // roxo: inquieto
        return 0xFFB0213A;                   // vermelho: à beira da loucura
    }

    private SanityHud() {}
}
