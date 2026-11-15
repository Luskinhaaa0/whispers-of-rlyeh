package com.whispersofrlyeh.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.whispersofrlyeh.space.SpaceBodies;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Céu do Espaço: preto, cheio de estrelas, e com cada astro desenhado como um disco
 * do tamanho aparente certo (calculado pela distância real), colorido com as cores
 * dos blocos verdadeiros do planeta e iluminado pelo lado que dá para o Sol.
 *
 * Longe: você vê o disco. Perto: os blocos reais carregam por cima, no mesmo lugar e
 * tamanho, e o disco continua "preenchendo" as partes além da distância de renderização.
 */
public class SpaceSkyEffects extends DimensionSpecialEffects {

    private static final float SKY_DISTANCE = 100f;
    private static final int RINGS = 10;
    private static final int SEGMENTS = 48;
    private static final float[][] STARS = makeStars(2200, 10842L);

    public SpaceSkyEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    // ---------- Sem névoa e sem nascer do sol (nuvens/chuva já não existem: bioma sem chuva, cloudLevel NaN) ----------

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return Vec3.ZERO;
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        return null;
    }

    // ---------- O céu ----------

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix,
                             Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        PoseStack pose = new PoseStack();
        pose.mulPose(modelViewMatrix);
        Matrix4f m = pose.last().pose();
        Vec3 cam = camera.getPosition();

        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        drawStars(buf, m, ticks + partialTick);

        // Do mais distante para o mais próximo, para os de perto ficarem na frente
        List<SpaceBodies.Body> bodies = new ArrayList<>(SpaceBodies.BODIES);
        bodies.sort(Comparator.comparingDouble((SpaceBodies.Body b) -> b.distanceTo(cam.x, cam.y, cam.z)).reversed());
        for (SpaceBodies.Body body : bodies) {
            drawBody(buf, m, cam, body);
        }
        BufferUploader.drawWithShader(buf.buildOrThrow());

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        return true;
    }

    private static float[][] makeStars(int count, long seed) {
        RandomSource r = RandomSource.create(seed);
        float[][] stars = new float[count][6];
        for (int i = 0; i < count; i++) {
            double x = r.nextGaussian(), y = r.nextGaussian(), z = r.nextGaussian();
            double len = Math.sqrt(x * x + y * y + z * z);
            stars[i][0] = (float) (x / len);
            stars[i][1] = (float) (y / len);
            stars[i][2] = (float) (z / len);
            stars[i][3] = 0.12f + r.nextFloat() * r.nextFloat() * 0.35f;   // tamanho
            stars[i][4] = 0.45f + r.nextFloat() * 0.55f;                    // brilho
            stars[i][5] = r.nextFloat();                                     // matiz
        }
        return stars;
    }

    private static void drawStars(BufferBuilder buf, Matrix4f m, float time) {
        for (int i = 0; i < STARS.length; i++) {
            float[] s = STARS[i];
            Vector3f dir = new Vector3f(s[0], s[1], s[2]);
            Vector3f[] basis = basis(dir);
            float size = s[3];
            float twinkle = 0.85f + 0.15f * (float) Math.sin(time * 0.05f + i);
            float b = s[4] * twinkle;
            float red = b * (s[5] > 0.8f ? 0.8f : 1f);
            float green = b * (s[5] > 0.9f ? 0.85f : 1f);
            float blue = b * (s[5] < 0.15f ? 0.75f : 1f);
            Vector3f c = new Vector3f(dir).mul(SKY_DISTANCE);
            quad(buf, m, c, basis[0], basis[1], size, red, green, blue, 1f);
        }
    }

    private static void drawBody(BufferBuilder buf, Matrix4f m, Vec3 cam, SpaceBodies.Body body) {
        double dx = body.cx() + 0.5 - cam.x, dy = body.cy() + 0.5 - cam.y, dz = body.cz() + 0.5 - cam.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double r = body.radius();
        if (dist < r * 1.12) return; // em cima do planeta: os blocos reais bastam

        Vector3f dir = new Vector3f((float) (dx / dist), (float) (dy / dist), (float) (dz / dist));
        Vector3f[] basis = basis(dir);
        Vector3f u = basis[0], w = basis[1];
        double alpha = Math.asin(Math.min(0.995, r / dist));
        float discR = (float) (SKY_DISTANCE * Math.tan(Math.min(alpha, Math.toRadians(80))));
        Vector3f center = new Vector3f(dir).mul(SKY_DISTANCE);

        boolean emissive = body.kind() == SpaceBodies.Kind.SUN;
        Vector3f toSun = new Vector3f(
                (float) (SpaceBodies.SUN.cx() - body.cx()),
                (float) (SpaceBodies.SUN.cy() - body.cy()),
                (float) (SpaceBodies.SUN.cz() - body.cz())).normalize();

        // Halo do Sol
        if (emissive) {
            for (int s = 0; s < SEGMENTS; s++) {
                double a0 = 2 * Math.PI * s / SEGMENTS, a1 = 2 * Math.PI * (s + 1) / SEGMENTS;
                Vector3f p0 = ring(center, u, w, discR, a0), p1 = ring(center, u, w, discR, a1);
                Vector3f q0 = ring(center, u, w, discR * 2.4f, a0), q1 = ring(center, u, w, discR * 2.4f, a1);
                vtx(buf, m, p0, 1f, 0.8f, 0.35f, 0.55f);
                vtx(buf, m, q0, 1f, 0.6f, 0.2f, 0f);
                vtx(buf, m, q1, 1f, 0.6f, 0.2f, 0f);
                vtx(buf, m, p1, 1f, 0.8f, 0.35f, 0.55f);
            }
        }

        // Disco em anéis concêntricos: cada vértice pega a cor do bloco real e a luz do Sol
        float[][][] col = new float[RINGS + 1][SEGMENTS + 1][];
        Vector3f[][] p = new Vector3f[RINGS + 1][SEGMENTS + 1];
        for (int k = 0; k <= RINGS; k++) {
            float t = (float) k / RINGS;
            for (int s = 0; s <= SEGMENTS; s++) {
                double a = 2 * Math.PI * s / SEGMENTS;
                float sx = (float) (t * Math.cos(a)), sy = (float) (t * Math.sin(a));
                float sz = (float) Math.sqrt(Math.max(0, 1 - sx * sx - sy * sy));
                // normal da esfera no ponto visível (virada para a câmera)
                Vector3f n = new Vector3f(u).mul(sx).add(new Vector3f(w).mul(sy)).add(new Vector3f(dir).mul(-sz)).normalize();
                p[k][s] = ring(center, u, w, discR * t, a);
                col[k][s] = surfaceColor(body, n, toSun, emissive);
            }
        }
        for (int k = 0; k < RINGS; k++) {
            for (int s = 0; s < SEGMENTS; s++) {
                vtx(buf, m, p[k][s], col[k][s]);
                vtx(buf, m, p[k + 1][s], col[k + 1][s]);
                vtx(buf, m, p[k + 1][s + 1], col[k + 1][s + 1]);
                vtx(buf, m, p[k][s + 1], col[k][s + 1]);
            }
        }
    }

    /** Cor do bloco real na superfície naquele ponto, com sombreamento pelo Sol. */
    private static float[] surfaceColor(SpaceBodies.Body body, Vector3f n, Vector3f toSun, boolean emissive) {
        double px = body.cx() + n.x * (body.radius() - 0.6);
        double py = body.cy() + n.y * (body.radius() - 0.6);
        double pz = body.cz() + n.z * (body.radius() - 0.6);
        BlockState state = SpaceBodies.blockFor(body, px, py, pz);
        if (state == null) { // cratera/cânion: olha um pouco mais fundo
            state = SpaceBodies.blockFor(body, body.cx() + n.x * (body.radius() - 6), body.cy() + n.y * (body.radius() - 6),
                    body.cz() + n.z * (body.radius() - 6));
        }
        int rgb = state == null ? 0x808080 : state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).col;
        float r = ((rgb >> 16) & 0xFF) / 255f, g = ((rgb >> 8) & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        if (emissive) {
            return new float[]{Math.min(1f, r * 1.25f + 0.15f), Math.min(1f, g * 1.2f + 0.1f), Math.min(1f, b + 0.05f), 1f};
        }
        float light = 0.08f + 0.92f * Math.max(0f, n.dot(toSun));
        return new float[]{r * light, g * light, b * light, 1f};
    }

    // ---------- utilidades de geometria ----------

    private static Vector3f[] basis(Vector3f dir) {
        Vector3f up = Math.abs(dir.y) < 0.99f ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        Vector3f u = new Vector3f(dir).cross(up).normalize();
        Vector3f w = new Vector3f(u).cross(dir).normalize();
        return new Vector3f[]{u, w};
    }

    private static Vector3f ring(Vector3f center, Vector3f u, Vector3f w, float radius, double angle) {
        return new Vector3f(center)
                .add(new Vector3f(u).mul((float) (radius * Math.cos(angle))))
                .add(new Vector3f(w).mul((float) (radius * Math.sin(angle))));
    }

    private static void quad(BufferBuilder buf, Matrix4f m, Vector3f c, Vector3f u, Vector3f w, float size,
                             float r, float g, float b, float a) {
        Vector3f du = new Vector3f(u).mul(size), dw = new Vector3f(w).mul(size);
        vtx(buf, m, new Vector3f(c).sub(du).sub(dw), r, g, b, a);
        vtx(buf, m, new Vector3f(c).add(du).sub(dw), r, g, b, a);
        vtx(buf, m, new Vector3f(c).add(du).add(dw), r, g, b, a);
        vtx(buf, m, new Vector3f(c).sub(du).add(dw), r, g, b, a);
    }

    private static void vtx(BufferBuilder buf, Matrix4f m, Vector3f p, float[] c) {
        vtx(buf, m, p, c[0], c[1], c[2], c[3]);
    }

    private static void vtx(BufferBuilder buf, Matrix4f m, Vector3f p, float r, float g, float b, float a) {
        buf.addVertex(m, p.x, p.y, p.z).setColor(r, g, b, a);
    }
}
