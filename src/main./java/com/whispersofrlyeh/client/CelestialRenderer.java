package com.whispersofrlyeh.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.whispersofrlyeh.space.Planet;
import com.whispersofrlyeh.space.SpaceBodies;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Desenho dos corpos celestes no céu (só visual): estrelas, Sol com halo, planetas iluminados
 * pelo Sol com cores procedurais, anéis de Saturno e cometas com cauda.
 *
 * Tudo é desenhado numa "esfera celeste" a SKY_DISTANCE da câmera, com o tamanho aparente
 * calculado pela distância real (então os planetas crescem conforme você se aproxima).
 */
public final class CelestialRenderer {

    public static final float SKY_DISTANCE = 100f;
    private static final SimplexNoise NOISE = new SimplexNoise(RandomSource.create(0x5EA5EAL));
    private static final float[][] STARS = makeStars(2400, 10842L);

    /** Tipos de superfície para o colorido procedural. */
    public enum Look { SUN, EARTH, MOON, MERCURY, VENUS, MARS, JUPITER, SATURN, URANUS, NEPTUNE }

    public static Look lookOf(Planet p) {
        return switch (p) {
            case MERCURY -> Look.MERCURY;
            case VENUS -> Look.VENUS;
            case MOON -> Look.MOON;
            case MARS -> Look.MARS;
            case JUPITER -> Look.JUPITER;
            case SATURN -> Look.SATURN;
            case URANUS -> Look.URANUS;
            case NEPTUNE -> Look.NEPTUNE;
        };
    }

    // ---------------------------------------------------------------------
    // Estrelas
    // ---------------------------------------------------------------------

    private static float[][] makeStars(int count, long seed) {
        RandomSource r = RandomSource.create(seed);
        float[][] stars = new float[count][6];
        for (int i = 0; i < count; i++) {
            double x = r.nextGaussian(), y = r.nextGaussian(), z = r.nextGaussian();
            double len = Math.sqrt(x * x + y * y + z * z);
            stars[i][0] = (float) (x / len);
            stars[i][1] = (float) (y / len);
            stars[i][2] = (float) (z / len);
            stars[i][3] = 0.12f + r.nextFloat() * r.nextFloat() * 0.35f;
            stars[i][4] = 0.45f + r.nextFloat() * 0.55f;
            stars[i][5] = r.nextFloat();
        }
        return stars;
    }

    public static void drawStars(BufferBuilder buf, Matrix4f m, float time, float alpha) {
        for (int i = 0; i < STARS.length; i++) {
            float[] s = STARS[i];
            Vector3f dir = new Vector3f(s[0], s[1], s[2]);
            Vector3f[] basis = basis(dir);
            float twinkle = 0.85f + 0.15f * (float) Math.sin(time * 0.05f + i);
            float b = s[4] * twinkle;
            float red = b * (s[5] > 0.8f ? 0.8f : 1f);
            float green = b * (s[5] > 0.9f ? 0.85f : 1f);
            float blue = b * (s[5] < 0.15f ? 0.75f : 1f);
            quad(buf, m, new Vector3f(dir).mul(SKY_DISTANCE), basis[0], basis[1], s[3], red, green, blue, alpha);
        }
    }

    // ---------------------------------------------------------------------
    // Esferas (planetas, Sol, Terra)
    // ---------------------------------------------------------------------

    /**
     * Desenha uma esfera vista de (camX, camY, camZ).
     * @param sunX/sunY/sunZ  posição do Sol, para a luz e as fases
     */
    public static void drawSphere(BufferBuilder buf, Matrix4f m, double camX, double camY, double camZ,
                                  double cx, double cy, double cz, double radius, Look look,
                                  double sunX, double sunY, double sunZ, int rings, int segments) {
        double dx = cx - camX, dy = cy - camY, dz = cz - camZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < radius * 1.02) return;

        Vector3f dir = new Vector3f((float) (dx / dist), (float) (dy / dist), (float) (dz / dist));
        Vector3f[] b = basis(dir);
        Vector3f u = b[0], w = b[1];
        double alpha = Math.asin(Math.min(0.999, radius / dist));
        // planetas muito distantes nunca somem: no mínimo um pontinho
        float discR = (float) Math.max(0.12, SKY_DISTANCE * Math.tan(Math.min(alpha, Math.toRadians(84))));
        Vector3f center = new Vector3f(dir).mul(SKY_DISTANCE);
        boolean emissive = look == Look.SUN;

        Vector3f toSun = new Vector3f((float) (sunX - cx), (float) (sunY - cy), (float) (sunZ - cz));
        if (toSun.lengthSquared() < 1e-6f) toSun.set(0, 1, 0);
        toSun.normalize();

        if (emissive) {
            halo(buf, m, center, u, w, discR, 2.6f, 1f, 0.78f, 0.35f, 0.55f, segments);
        } else if (look == Look.EARTH || look == Look.VENUS || look == Look.NEPTUNE || look == Look.URANUS) {
            // brilho fininho de atmosfera
            float[] c = look == Look.VENUS ? new float[]{1f, 0.9f, 0.6f} : new float[]{0.45f, 0.7f, 1f};
            halo(buf, m, center, u, w, discR, 1.06f, c[0], c[1], c[2], 0.35f, segments);
        }

        Vector3f[][] p = new Vector3f[rings + 1][segments + 1];
        float[][][] col = new float[rings + 1][segments + 1][];
        for (int k = 0; k <= rings; k++) {
            float t = (float) k / rings;
            for (int s = 0; s <= segments; s++) {
                double a = 2 * Math.PI * s / segments;
                float sx = (float) (t * Math.cos(a)), sy = (float) (t * Math.sin(a));
                float sz = (float) Math.sqrt(Math.max(0, 1 - sx * sx - sy * sy));
                Vector3f n = new Vector3f(u).mul(sx).add(new Vector3f(w).mul(sy)).add(new Vector3f(dir).mul(-sz)).normalize();
                p[k][s] = ring(center, u, w, discR * t, a);
                float[] base = surfaceColor(look, n);
                if (emissive) {
                    col[k][s] = base;
                } else {
                    float light = 0.06f + 0.94f * Math.max(0f, n.dot(toSun));
                    float rim = 1f - 0.25f * t * t; // borda levemente mais escura
                    col[k][s] = new float[]{base[0] * light * rim, base[1] * light * rim, base[2] * light * rim, 1f};
                }
            }
        }
        for (int k = 0; k < rings; k++) {
            for (int s = 0; s < segments; s++) {
                vtx(buf, m, p[k][s], col[k][s]);
                vtx(buf, m, p[k + 1][s], col[k + 1][s]);
                vtx(buf, m, p[k + 1][s + 1], col[k + 1][s + 1]);
                vtx(buf, m, p[k][s + 1], col[k][s + 1]);
            }
        }
    }

    private static float n3(double x, double y, double z) {
        return (float) NOISE.getValue(x, y, z);
    }

    /** Cor base de um ponto da superfície (normal n). */
    public static float[] surfaceColor(Look look, Vector3f n) {
        float nx = n.x, ny = n.y, nz = n.z;
        switch (look) {
            case SUN -> {
                float v = n3(nx * 6, ny * 6, nz * 6) * 0.5f + n3(nx * 18, ny * 18, nz * 18) * 0.25f;
                return new float[]{1f, Math.min(1f, 0.78f + v * 0.2f), Math.min(1f, 0.35f + v * 0.25f), 1f};
            }
            case EARTH -> {
                float c = n3(nx * 2.2, ny * 2.2, nz * 2.2) + 0.5f * n3(nx * 5 + 10, ny * 5, nz * 5);
                float cloud = n3(nx * 4 + 40, ny * 8, nz * 4);
                float[] base;
                if (Math.abs(ny) > 0.85f) base = new float[]{0.92f, 0.95f, 0.98f};
                else if (c > 0.05f) base = (Math.abs(ny) < 0.3f && c > 0.35f) ? new float[]{0.82f, 0.74f, 0.5f}
                        : new float[]{0.25f, 0.55f, 0.22f};
                else base = new float[]{0.12f, 0.28f, 0.72f};
                if (cloud > 0.35f) base = mixc(base, new float[]{1f, 1f, 1f}, Math.min(1f, (cloud - 0.35f) * 3f));
                return new float[]{base[0], base[1], base[2], 1f};
            }
            case MOON -> {
                float v = n3(nx * 3, ny * 3, nz * 3);
                float g = v > 0.25f ? 0.45f : 0.72f + n3(nx * 12, ny * 12, nz * 12) * 0.06f;
                return new float[]{g, g, g * 0.97f, 1f};
            }
            case MERCURY -> {
                float g = 0.5f + n3(nx * 8, ny * 8, nz * 8) * 0.1f;
                return new float[]{g * 1.05f, g * 0.95f, g * 0.88f, 1f};
            }
            case VENUS -> {
                float v = n3(nx * 2 + ny, ny * 4, nz * 2) * 0.12f;
                return new float[]{0.93f + v, 0.83f + v, 0.55f + v, 1f};
            }
            case MARS -> {
                if (ny > 0.9f) return new float[]{0.95f, 0.95f, 0.98f, 1f};
                float v = n3(nx * 3, ny * 3, nz * 3);
                return v > 0.3f ? new float[]{0.5f, 0.22f, 0.13f, 1f} : new float[]{0.8f, 0.38f, 0.2f, 1f};
            }
            case JUPITER -> {
                double lon = Math.atan2(nz, nx);
                double spot = Math.pow((ny + 0.3) / 0.1, 2) + Math.pow((lon - 0.8) / 0.25, 2);
                if (spot < 1) return new float[]{0.72f, 0.28f, 0.18f, 1f};
                float lat = ny + n3(nx * 3, ny * 3, nz * 3) * 0.05f;
                float band = (float) Math.sin(lat * 26);
                return band > 0 ? new float[]{0.92f, 0.85f, 0.72f, 1f} : new float[]{0.72f, 0.5f, 0.32f, 1f};
            }
            case SATURN -> {
                float band = (float) Math.sin((ny + n3(nx * 3, ny * 3, nz * 3) * 0.03f) * 22);
                return band > 0 ? new float[]{0.92f, 0.84f, 0.6f, 1f} : new float[]{0.82f, 0.7f, 0.46f, 1f};
            }
            case URANUS -> {
                return new float[]{0.62f, 0.86f, 0.88f, 1f};
            }
            case NEPTUNE -> {
                float v = n3(nx * 3, ny * 6, nz * 3) * 0.08f;
                return new float[]{0.2f + v, 0.36f + v, 0.82f + v, 1f};
            }
        }
        return new float[]{0.5f, 0.5f, 0.5f, 1f};
    }

    private static float[] mixc(float[] a, float[] b, float t) {
        return new float[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    private static void halo(BufferBuilder buf, Matrix4f m, Vector3f center, Vector3f u, Vector3f w, float r,
                             float scale, float cr, float cg, float cb, float alpha, int segments) {
        for (int s = 0; s < segments; s++) {
            double a0 = 2 * Math.PI * s / segments, a1 = 2 * Math.PI * (s + 1) / segments;
            vtx(buf, m, ring(center, u, w, r, a0), cr, cg, cb, alpha);
            vtx(buf, m, ring(center, u, w, r * scale, a0), cr, cg, cb, 0f);
            vtx(buf, m, ring(center, u, w, r * scale, a1), cr, cg, cb, 0f);
            vtx(buf, m, ring(center, u, w, r, a1), cr, cg, cb, alpha);
        }
    }

    // ---------------------------------------------------------------------
    // Anéis de Saturno (geometria 3D de verdade, projetada no céu)
    // ---------------------------------------------------------------------

    /**
     * @param front true = desenha só a parte do anel mais perto da câmera que o planeta;
     *              false = só a parte de trás. Desenhe "trás", depois o planeta, depois "frente".
     */
    public static void drawRings(BufferBuilder buf, Matrix4f m, double camX, double camY, double camZ,
                                 double cx, double cy, double cz, double planetR, boolean front) {
        drawRings(buf, m, camX, camY, camZ, cx, cy, cz, planetR, front, 20);
    }

    public static void drawRings(BufferBuilder buf, Matrix4f m, double camX, double camY, double camZ,
                                 double cx, double cy, double cz, double planetR, boolean front, double tiltDegrees) {
        double centerDist = Math.sqrt((cx - camX) * (cx - camX) + (cy - camY) * (cy - camY) + (cz - camZ) * (cz - camZ));
        double tilt = Math.toRadians(tiltDegrees);
        Vector3f ax = new Vector3f(1, 0, 0);
        Vector3f az = new Vector3f(0, (float) Math.sin(tilt), (float) Math.cos(tilt));
        int seg = 96;
        double[][] bands = {{1.25, 1.55, 0.85, 0.78, 0.6, 0.55}, {1.6, 2.05, 0.9, 0.83, 0.66, 0.75}, {2.12, 2.3, 0.78, 0.72, 0.58, 0.45}};
        for (double[] band : bands) {
            for (int s = 0; s < seg; s++) {
                double a0 = 2 * Math.PI * s / seg, a1 = 2 * Math.PI * (s + 1) / seg;
                double[] p00 = ringPoint(cx, cy, cz, ax, az, planetR * band[0], a0);
                double[] p10 = ringPoint(cx, cy, cz, ax, az, planetR * band[1], a0);
                double[] p11 = ringPoint(cx, cy, cz, ax, az, planetR * band[1], a1);
                double[] p01 = ringPoint(cx, cy, cz, ax, az, planetR * band[0], a1);
                double dMid = dist(p00, camX, camY, camZ);
                boolean isFront = dMid < centerDist;
                if (isFront != front) continue;
                float r = (float) band[2], g = (float) band[3], b = (float) band[4], a = (float) band[5];
                vtx(buf, m, project(p00, camX, camY, camZ), r, g, b, a);
                vtx(buf, m, project(p10, camX, camY, camZ), r, g, b, a);
                vtx(buf, m, project(p11, camX, camY, camZ), r, g, b, a);
                vtx(buf, m, project(p01, camX, camY, camZ), r, g, b, a);
            }
        }
    }

    private static double[] ringPoint(double cx, double cy, double cz, Vector3f ax, Vector3f az, double r, double a) {
        double c = Math.cos(a) * r, s = Math.sin(a) * r;
        return new double[]{cx + ax.x * c + az.x * s, cy + ax.y * c + az.y * s, cz + ax.z * c + az.z * s};
    }

    private static double dist(double[] p, double x, double y, double z) {
        return Math.sqrt((p[0] - x) * (p[0] - x) + (p[1] - y) * (p[1] - y) + (p[2] - z) * (p[2] - z));
    }

    /** Projeta um ponto do mundo na esfera celeste. */
    public static Vector3f project(double[] p, double camX, double camY, double camZ) {
        Vector3f v = new Vector3f((float) (p[0] - camX), (float) (p[1] - camY), (float) (p[2] - camZ));
        if (v.lengthSquared() < 1e-6f) v.set(0, 1, 0);
        return v.normalize().mul(SKY_DISTANCE);
    }

    // ---------------------------------------------------------------------
    // Cometas
    // ---------------------------------------------------------------------

    /** Órbitas: centro (x,y,z), semi-eixos a/b, velocidade, fase */
    private static final double[][] COMETS = {
            {0, 300, -6000, 16000, 5000, 0.0009, 0.0},
            {2000, 260, 8000, 22000, 7000, 0.0006, 2.1},
            {-3000, 330, 2000, 12000, 9000, 0.0011, 4.0},
            {1000, 280, 15000, 30000, 6000, 0.0004, 1.3},
    };

    public static void drawComets(BufferBuilder buf, Matrix4f m, double camX, double camY, double camZ, double time) {
        for (double[] c : COMETS) {
            double ang = time * c[5] + c[6];
            double hx = c[0] + Math.cos(ang) * c[3];
            double hz = c[2] + Math.sin(ang) * c[4];
            double hy = c[1] + Math.sin(ang * 2) * 120;
            double[] head = {hx, hy, hz};
            // cauda aponta para longe do Sol
            Vector3f away = new Vector3f((float) (hx - SpaceBodies.SUN_X), (float) (hy - SpaceBodies.SUN_Y),
                    (float) (hz - SpaceBodies.SUN_Z)).normalize();
            double tailLen = 1800;
            int steps = 14;
            Vector3f headDir = project(head, camX, camY, camZ);
            double headDist = dist(head, camX, camY, camZ);
            float headSize = (float) Math.max(0.25, SKY_DISTANCE * 30 / headDist);
            Vector3f[] hb = basis(new Vector3f(headDir).normalize());
            quad(buf, m, headDir, hb[0], hb[1], headSize, 0.85f, 0.95f, 1f, 1f);
            quad(buf, m, headDir, hb[0], hb[1], headSize * 2.5f, 0.6f, 0.85f, 1f, 0.25f);

            for (int i = 0; i < steps; i++) {
                double t0 = (double) i / steps, t1 = (double) (i + 1) / steps;
                double[] a = {hx + away.x * tailLen * t0, hy + away.y * tailLen * t0, hz + away.z * tailLen * t0};
                double[] b = {hx + away.x * tailLen * t1, hy + away.y * tailLen * t1, hz + away.z * tailLen * t1};
                Vector3f pa = project(a, camX, camY, camZ), pb = project(b, camX, camY, camZ);
                Vector3f side = new Vector3f(pb).sub(pa).cross(new Vector3f(pa).normalize());
                if (side.lengthSquared() < 1e-8f) continue;
                side.normalize();
                float wa = headSize * (float) (1.0 + t0 * 2.5), wb = headSize * (float) (1.0 + t1 * 2.5);
                float alphaA = (float) (0.55 * (1 - t0)), alphaB = (float) (0.55 * (1 - t1));
                vtx(buf, m, new Vector3f(pa).add(new Vector3f(side).mul(wa)), 0.6f, 0.85f, 1f, alphaA);
                vtx(buf, m, new Vector3f(pa).sub(new Vector3f(side).mul(wa)), 0.6f, 0.85f, 1f, alphaA);
                vtx(buf, m, new Vector3f(pb).sub(new Vector3f(side).mul(wb)), 0.6f, 0.85f, 1f, alphaB);
                vtx(buf, m, new Vector3f(pb).add(new Vector3f(side).mul(wb)), 0.6f, 0.85f, 1f, alphaB);
            }
        }
    }

    // ---------------------------------------------------------------------
    // Geometria
    // ---------------------------------------------------------------------

    public static Vector3f[] basis(Vector3f dir) {
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

    public static void quad(BufferBuilder buf, Matrix4f m, Vector3f c, Vector3f u, Vector3f w, float size,
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

    public static void vtx(BufferBuilder buf, Matrix4f m, Vector3f p, float r, float g, float b, float a) {
        buf.addVertex(m, p.x, p.y, p.z).setColor(r, g, b, a);
    }

    private CelestialRenderer() {}
}
