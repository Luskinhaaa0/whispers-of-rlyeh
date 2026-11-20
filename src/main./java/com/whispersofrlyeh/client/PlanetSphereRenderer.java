package com.whispersofrlyeh.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.whispersofrlyeh.WhispersOfRlyeh;
import com.whispersofrlyeh.space.Planet;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Desenha planetas como ESFERAS 3D de verdade no céu, com texturas realistas:
 *
 *  1. superfície (textura do planeta) iluminada pelo Sol: lado do dia e lado da noite
 *  2. nuvens (Terra), numa esfera um pouco maior e girando mais rápido
 *  3. luzes das cidades no lado da noite (Terra), somadas por cima
 *  4. brilho da atmosfera na borda (efeito "Fresnel") + halo fininho em volta
 *
 * Cada vértice da esfera é calculado no mundo e projetado na esfera celeste, então o
 * planeta tem o tamanho e a perspectiva certos de qualquer distância.
 */
public final class PlanetSphereRenderer {

    private static final float D = CelestialRenderer.SKY_DISTANCE;

    public record Style(ResourceLocation surface, ResourceLocation night, ResourceLocation clouds,
                        float[] atmosphere, float atmosphereStrength, boolean emissive, float spin) {}

    private static ResourceLocation tex(String name) {
        return WhispersOfRlyeh.id("textures/environment/planets/" + name + ".png");
    }

    public static final Style SUN = new Style(tex("sun"), null, null, new float[]{1f, 0.72f, 0.3f}, 1f, true, 0.0004f);
    public static final Style EARTH = new Style(tex("earth_day"), tex("earth_night"), tex("earth_clouds"),
            new float[]{0.35f, 0.6f, 1f}, 1.4f, false, 0.0015f);
    public static final Style MOON = new Style(tex("moon"), null, null, null, 0f, false, 0.0004f);
    public static final Style MERCURY = new Style(tex("mercury"), null, null, null, 0f, false, 0.0003f);
    public static final Style VENUS = new Style(tex("venus"), null, null, new float[]{1f, 0.85f, 0.55f}, 1.2f, false, 0.0002f);
    public static final Style MARS = new Style(tex("mars"), null, null, new float[]{0.95f, 0.55f, 0.35f}, 0.7f, false, 0.0012f);
    public static final Style JUPITER = new Style(tex("jupiter"), null, null, new float[]{0.95f, 0.82f, 0.6f}, 0.9f, false, 0.0030f);
    public static final Style SATURN = new Style(tex("saturn"), null, null, new float[]{0.95f, 0.86f, 0.62f}, 0.8f, false, 0.0028f);
    public static final Style URANUS = new Style(tex("uranus"), null, null, new float[]{0.55f, 0.85f, 0.92f}, 1.0f, false, 0.0020f);
    public static final Style NEPTUNE = new Style(tex("neptune"), null, null, new float[]{0.4f, 0.6f, 1f}, 1.1f, false, 0.0022f);

    public static Style styleOf(Planet p) {
        return switch (p) {
            case MERCURY -> MERCURY;
            case VENUS -> VENUS;
            case MOON -> MOON;
            case MARS -> MARS;
            case JUPITER -> JUPITER;
            case SATURN -> SATURN;
            case URANUS -> URANUS;
            case NEPTUNE -> NEPTUNE;
        };
    }

    /**
     * @param rotation giro extra (em radianos) somado ao giro natural do planeta
     */
    public static void draw(Matrix4f m, double camX, double camY, double camZ,
                            double cx, double cy, double cz, double radius, Style style,
                            double sunX, double sunY, double sunZ, float time, double rotation) {
        double dx = cx - camX, dy = cy - camY, dz = cz - camZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < radius * 1.02) return;

        double angular = Math.asin(Math.min(0.999, radius / dist));
        int lat = angular > Math.toRadians(20) ? 48 : angular > Math.toRadians(3) ? 32 : 16;
        int lon = lat * 2;

        Vector3f toSun = new Vector3f((float) (sunX - cx), (float) (sunY - cy), (float) (sunZ - cz));
        if (toSun.lengthSquared() < 1e-6f) toSun.set(0, 1, 0);
        toSun.normalize();

        double spin = time * style.spin() + rotation;

        // 1. superfície
        Grid surface = grid(camX, camY, camZ, cx, cy, cz, radius, lat, lon, spin, toSun);
        beginTextured(style.surface());
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < lat; i++) {
            for (int j = 0; j < lon; j++) {
                if (!surface.visible(i, j)) continue;
                for (int[] c : CORNERS) {
                    int a = i + c[0], b = j + c[1];
                    float l = style.emissive() ? 1f : 0.03f + 0.97f * (float) Math.sqrt(Math.max(0f, surface.light[a][b]));
                    texVertex(buf, m, surface.pos[a][b], (float) b / lon, 1f - (float) a / lat, l, l, l, 1f);
                }
            }
        }
        upload(buf);

        // 2. nuvens
        if (style.clouds() != null) {
            Grid clouds = grid(camX, camY, camZ, cx, cy, cz, radius * 1.006, lat, lon, spin * 1.18 + 0.7, toSun);
            beginTextured(style.clouds());
            buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (int i = 0; i < lat; i++) {
                for (int j = 0; j < lon; j++) {
                    if (!clouds.visible(i, j)) continue;
                    for (int[] c : CORNERS) {
                        int a = i + c[0], b = j + c[1];
                        float l = 0.02f + 0.98f * (float) Math.sqrt(Math.max(0f, clouds.light[a][b]));
                        texVertex(buf, m, clouds.pos[a][b], (float) b / lon, 1f - (float) a / lat, l, l, l, 1f);
                    }
                }
            }
            upload(buf);
        }

        // 3. luzes das cidades (somadas)
        if (style.night() != null) {
            beginTextured(style.night());
            RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (int i = 0; i < lat; i++) {
                for (int j = 0; j < lon; j++) {
                    if (!surface.visible(i, j)) continue;
                    for (int[] c : CORNERS) {
                        int a = i + c[0], b = j + c[1];
                        float k = Math.max(0f, Math.min(1f, (0.12f - surface.light[a][b]) / 0.3f)) * 1.3f;
                        texVertex(buf, m, surface.pos[a][b], (float) b / lon, 1f - (float) a / lat, k, k, k, 1f);
                    }
                }
            }
            upload(buf);
            RenderSystem.defaultBlendFunc();
        }

        // 4. atmosfera: brilho na borda + halo em volta
        if (style.atmosphere() != null) {
            float[] atm = style.atmosphere();
            float strength = style.atmosphereStrength();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            if (!style.emissive()) {
                for (int i = 0; i < lat; i++) {
                    for (int j = 0; j < lon; j++) {
                        if (!surface.visible(i, j)) continue;
                        for (int[] c : CORNERS) {
                            int a = i + c[0], b = j + c[1];
                            float f = (float) Math.pow(1f - Math.max(0f, surface.facing[a][b]), 3);
                            float glow = Math.max(0f, Math.min(1f, surface.light[a][b] + 0.35f));
                            float k = f * glow * strength;
                            colorVertex(buf, m, surface.pos[a][b], atm[0] * k, atm[1] * k, atm[2] * k, 1f);
                        }
                    }
                }
            }
            halo(buf, m, camX, camY, camZ, cx, cy, cz, radius, dist, angular, atm, toSun, style.emissive());
            upload(buf);
            RenderSystem.defaultBlendFunc();
        }
    }

    // ---------------------------------------------------------------------

    private static final int[][] CORNERS = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};

    /** Vértices da esfera já projetados no céu, com luz do Sol e quanto cada um "olha" para a câmera. */
    private static final class Grid {
        final Vector3f[][] pos;
        final float[][] light, facing;

        Grid(int lat, int lon) {
            pos = new Vector3f[lat + 1][lon + 1];
            light = new float[lat + 1][lon + 1];
            facing = new float[lat + 1][lon + 1];
        }

        /** Só desenha a face voltada para a câmera (a de trás fica escondida). */
        boolean visible(int i, int j) {
            return Math.max(Math.max(facing[i][j], facing[i + 1][j]), Math.max(facing[i + 1][j + 1], facing[i][j + 1])) > -0.02f;
        }
    }

    private static Grid grid(double camX, double camY, double camZ, double cx, double cy, double cz, double r,
                             int lat, int lon, double spin, Vector3f toSun) {
        Grid g = new Grid(lat, lon);
        for (int i = 0; i <= lat; i++) {
            double la = -Math.PI / 2 + Math.PI * i / lat;
            double cl = Math.cos(la), sl = Math.sin(la);
            for (int j = 0; j <= lon; j++) {
                double lo = 2 * Math.PI * j / lon - spin;
                double nx = cl * Math.cos(lo), ny = sl, nz = cl * Math.sin(lo);
                double px = cx + nx * r - camX, py = cy + ny * r - camY, pz = cz + nz * r - camZ;
                double len = Math.sqrt(px * px + py * py + pz * pz);
                if (len < 1e-6) len = 1e-6;
                g.pos[i][j] = new Vector3f((float) (px / len * D), (float) (py / len * D), (float) (pz / len * D));
                g.facing[i][j] = (float) (-(nx * px + ny * py + nz * pz) / len);
                g.light[i][j] = (float) (nx * toSun.x + ny * toSun.y + nz * toSun.z);
            }
        }
        return g;
    }

    /** Anel de brilho em volta da silhueta (no Sol, uma coroa enorme). */
    private static void halo(BufferBuilder buf, Matrix4f m, double camX, double camY, double camZ,
                             double cx, double cy, double cz, double radius, double dist, double angular,
                             float[] atm, Vector3f toSun, boolean emissive) {
        Vector3f dir = new Vector3f((float) ((cx - camX) / dist), (float) ((cy - camY) / dist), (float) ((cz - camZ) / dist));
        Vector3f[] basis = CelestialRenderer.basis(dir);
        Vector3f u = basis[0], w = basis[1];
        float discR = (float) (D * Math.tan(Math.min(angular, Math.toRadians(84))));
        Vector3f center = new Vector3f(dir).mul(D);
        float outer = emissive ? 2.8f : 1.07f;
        float inner = emissive ? 0.95f : 0.985f;
        int seg = 72;
        for (int s = 0; s < seg; s++) {
            double a0 = 2 * Math.PI * s / seg, a1 = 2 * Math.PI * (s + 1) / seg;
            float k0 = 1f, k1 = 1f;
            if (!emissive) {
                Vector3f n0 = new Vector3f(u).mul((float) Math.cos(a0)).add(new Vector3f(w).mul((float) Math.sin(a0)));
                Vector3f n1 = new Vector3f(u).mul((float) Math.cos(a1)).add(new Vector3f(w).mul((float) Math.sin(a1)));
                k0 = Math.max(0.12f, Math.min(1f, n0.dot(toSun) * 0.6f + 0.5f));
                k1 = Math.max(0.12f, Math.min(1f, n1.dot(toSun) * 0.6f + 0.5f));
            }
            float base = emissive ? 0.9f : 0.75f;
            colorVertex(buf, m, ring(center, u, w, discR * inner, a0), atm[0] * base * k0, atm[1] * base * k0, atm[2] * base * k0, 1f);
            colorVertex(buf, m, ring(center, u, w, discR * outer, a0), 0, 0, 0, 1f);
            colorVertex(buf, m, ring(center, u, w, discR * outer, a1), 0, 0, 0, 1f);
            colorVertex(buf, m, ring(center, u, w, discR * inner, a1), atm[0] * base * k1, atm[1] * base * k1, atm[2] * base * k1, 1f);
        }
    }

    private static Vector3f ring(Vector3f center, Vector3f u, Vector3f w, float radius, double angle) {
        return new Vector3f(center)
                .add(new Vector3f(u).mul((float) (radius * Math.cos(angle))))
                .add(new Vector3f(w).mul((float) (radius * Math.sin(angle))));
    }

    private static void beginTextured(ResourceLocation texture) {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture);
    }

    private static void texVertex(BufferBuilder buf, Matrix4f m, Vector3f p, float u, float v,
                                  float r, float g, float b, float a) {
        buf.addVertex(m, p.x, p.y, p.z).setUv(u, v).setColor(r, g, b, a);
    }

    private static void colorVertex(BufferBuilder buf, Matrix4f m, Vector3f p, float r, float g, float b, float a) {
        buf.addVertex(m, p.x, p.y, p.z).setColor(r, g, b, a);
    }

    private static void upload(BufferBuilder buf) {
        MeshData mesh = buf.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }

    private PlanetSphereRenderer() {}
}
