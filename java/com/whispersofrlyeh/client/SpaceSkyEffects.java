package com.whispersofrlyeh.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.whispersofrlyeh.space.Planet;
import com.whispersofrlyeh.space.SpaceBodies;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Céus "sem atmosfera":
 *  SPACE   : estrelas, cometas, o Sol, a Terra gigante lá embaixo e os planetas em 3D (com texturas)
 *  MOON    : céu preto com estrelas, a Terra pendurada no céu e o Sol
 *  MERCURY : céu preto com estrelas e um Sol enorme
 *  GAS     : sem estrelas (só a cor da névoa do planeta)
 *  SATURN  : como GAS, mas com os anéis cruzando o céu
 */
public class SpaceSkyEffects extends DimensionSpecialEffects {

    public enum Mode { SPACE, MOON, MERCURY, GAS, SATURN }

    private final Mode mode;

    public SpaceSkyEffects(Mode mode) {
        super(Float.NaN, false, SkyType.NONE, false, false);
        this.mode = mode;
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return (mode == Mode.GAS || mode == Mode.SATURN) ? fogColor : Vec3.ZERO;
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        return null;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix,
                             Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        PoseStack pose = new PoseStack();
        pose.mulPose(modelViewMatrix);
        Matrix4f m = pose.last().pose();
        Vec3 cam = camera.getPosition();
        float time = ticks + partialTick;

        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        switch (mode) {
            case SPACE -> drawSpace(m, cam, time);
            case MOON -> drawMoonSky(m, time);
            case MERCURY -> drawMercurySky(m, time);
            case SATURN -> colorPass(buf -> {
                // de dentro da atmosfera, os anéis aparecem como arcos cruzando o céu
                CelestialRenderer.drawRings(buf, m, 0, 0, 0, 0, -8000, 0, 8000, false, 55);
                CelestialRenderer.drawRings(buf, m, 0, 0, 0, 0, -8000, 0, 8000, true, 55);
            });
            case GAS -> { }
        }

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        return true;
    }

    /** Desenha algo só com cor (estrelas, cometas, anéis). */
    private static void colorPass(Consumer<BufferBuilder> drawer) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        drawer.accept(buf);
        MeshData mesh = buf.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }

    // ---------- Espaço ----------

    private record Drawable(double dist, Runnable draw) {}

    private static void drawSpace(Matrix4f m, Vec3 cam, float time) {
        colorPass(buf -> {
            CelestialRenderer.drawStars(buf, m, time, 1f);
            CelestialRenderer.drawComets(buf, m, cam.x, cam.y, cam.z, time);
        });

        double sx = SpaceBodies.SUN_X, sy = SpaceBodies.SUN_Y, sz = SpaceBodies.SUN_Z;
        List<Drawable> list = new ArrayList<>();

        list.add(new Drawable(dist(cam, sx, sy, sz), () -> PlanetSphereRenderer.draw(m, cam.x, cam.y, cam.z,
                sx, sy, sz, SpaceBodies.SUN_RADIUS, PlanetSphereRenderer.SUN, sx, sy + 1, sz, time, 0)));

        // Terra: sempre logo abaixo de você (você acabou de sair dela).
        // O giro acompanha seu X/Z, então andar pelo Espaço "passa" por cima dos continentes.
        double ey = SpaceBodies.EARTH_CENTER_Y;
        double earthR = SpaceBodies.EARTH_RADIUS;
        list.add(new Drawable(dist(cam, cam.x, ey, cam.z), () -> PlanetSphereRenderer.draw(m, cam.x, cam.y, cam.z,
                cam.x, ey, cam.z, earthR, PlanetSphereRenderer.EARTH, sx, sy, sz, time, cam.x / earthR)));

        for (Planet p : Planet.values()) {
            double d = dist(cam, p.cx, p.cy, p.cz);
            if (p == Planet.SATURN) {
                list.add(new Drawable(d + 0.5, () -> colorPass(buf -> CelestialRenderer.drawRings(buf, m,
                        cam.x, cam.y, cam.z, p.cx, p.cy, p.cz, p.radius, false))));
            }
            list.add(new Drawable(d, () -> PlanetSphereRenderer.draw(m, cam.x, cam.y, cam.z,
                    p.cx, p.cy, p.cz, p.radius, PlanetSphereRenderer.styleOf(p), sx, sy, sz, time, p.ordinal())));
            if (p == Planet.SATURN) {
                list.add(new Drawable(d - 0.5, () -> colorPass(buf -> CelestialRenderer.drawRings(buf, m,
                        cam.x, cam.y, cam.z, p.cx, p.cy, p.cz, p.radius, true))));
            }
        }

        list.sort(Comparator.comparingDouble(Drawable::dist).reversed());
        for (Drawable d : list) d.draw().run();
    }

    private static double dist(Vec3 cam, double x, double y, double z) {
        double dx = x - cam.x, dy = y - cam.y, dz = z - cam.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // ---------- Lua e Mercúrio ----------

    private static void drawMoonSky(Matrix4f m, float time) {
        colorPass(buf -> CelestialRenderer.drawStars(buf, m, time, 1f));
        double[] sun = {-0.4 * 60000, 0.8 * 60000, 0.45 * 60000};
        PlanetSphereRenderer.draw(m, 0, 0, 0, sun[0], sun[1], sun[2], 600, PlanetSphereRenderer.SUN,
                sun[0], sun[1] + 1, sun[2], time, 0);
        double[] earth = {0.35 * 20000, 0.55 * 20000, -0.76 * 20000};
        PlanetSphereRenderer.draw(m, 0, 0, 0, earth[0], earth[1], earth[2], 1400, PlanetSphereRenderer.EARTH,
                sun[0], sun[1], sun[2], time, 0);
    }

    private static void drawMercurySky(Matrix4f m, float time) {
        colorPass(buf -> CelestialRenderer.drawStars(buf, m, time, 1f));
        double[] sun = {0.3 * 20000, 0.75 * 20000, -0.6 * 20000};
        PlanetSphereRenderer.draw(m, 0, 0, 0, sun[0], sun[1], sun[2], 2000, PlanetSphereRenderer.SUN,
                sun[0], sun[1] + 1, sun[2], time, 0);
    }
}
