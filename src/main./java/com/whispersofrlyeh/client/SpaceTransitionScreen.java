package com.whispersofrlyeh.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Substitui VISUALMENTE a tela "Carregando terreno" nas viagens espaciais.
 * A tela original continua trabalhando por baixo (tick), então o jogo carrega o mundo
 * exatamente como sempre; só o que aparece é a nossa animação.
 */
public class SpaceTransitionScreen extends Screen {

    private final ReceivingLevelScreen inner;

    public SpaceTransitionScreen(ReceivingLevelScreen inner) {
        super(Component.empty());
        this.inner = inner;
    }

    @Override
    protected void init() {
        inner.init(this.minecraft, this.width, this.height);
    }

    @Override
    public void tick() {
        inner.tick(); // quando o mundo estiver pronto, ela mesma fecha a tela
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF000000);
        SpaceTransitionOverlay.drawTransition(graphics, this.width, this.height);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
