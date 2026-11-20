package com.whispersofrlyeh.space;

import com.whispersofrlyeh.WhispersOfRlyeh;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Os planetas visitáveis. Cada um tem:
 *  - uma DIMENSÃO própria (whispersofrlyeh:<id>) onde você pousa/flutua
 *  - uma esfera VISUAL no Espaço (centro + raio): voar para dentro dela = entrar no planeta
 *  - gravidade relativa à da Terra
 *
 * A Terra é o próprio Overworld (sai por Y > 600, volta caindo abaixo de Y 0 no Espaço).
 *
 *   Planeta    Centro no Espaço (X, Y, Z)   Raio   Tipo
 *   Mercúrio   (    0, 300, -14000)           70   sem ar
 *   Vênus      ( 2500, 280, -10500)          140   rochoso, infernal
 *   Lua        (  900, 300,    700)           90   sem ar
 *   Marte      (-3000, 300,   4000)          110   rochoso
 *   Júpiter    ( 5000, 300,  12000)          650   gasoso
 *   Saturno    (-7000, 300,  20000)          550   gasoso, com anéis
 *   Urano      ( 9000, 250,  28000)          280   gasoso, gelado
 *   Netuno     (-6000, 250,  36000)          270   gasoso, gelado
 *   (Sol em (0, 400, -18000), raio 3000: não dá para pousar, só para se queimar)
 */
public enum Planet {
    MERCURY("mercury", Kind.AIRLESS, 0.38, 0, 300, -14000, 70),
    VENUS("venus", Kind.ROCKY, 0.90, 2500, 280, -10500, 140),
    MOON("moon", Kind.AIRLESS, 0.17, 900, 300, 700, 90),
    MARS("mars", Kind.ROCKY, 0.38, -3000, 300, 4000, 110),
    JUPITER("jupiter", Kind.GAS, 1.60, 5000, 300, 12000, 650),
    SATURN("saturn", Kind.GAS, 1.10, -7000, 300, 20000, 550),
    URANUS("uranus", Kind.GAS, 0.90, 9000, 250, 28000, 280),
    NEPTUNE("neptune", Kind.GAS, 1.10, -6000, 250, 36000, 270);

    public enum Kind { AIRLESS, ROCKY, GAS }

    public final String id;
    public final Kind kind;
    public final double gravity;
    public final int cx, cy, cz, radius;
    private ResourceKey<Level> dimension;

    Planet(String id, Kind kind, double gravity, int cx, int cy, int cz, int radius) {
        this.id = id;
        this.kind = kind;
        this.gravity = gravity;
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        this.radius = radius;
    }

    public ResourceKey<Level> dimension() {
        if (dimension == null) {
            dimension = ResourceKey.create(Registries.DIMENSION, WhispersOfRlyeh.id(id));
        }
        return dimension;
    }

    public boolean isGas() {
        return kind == Kind.GAS;
    }

    /** Altura de chegada na dimensão do planeta. */
    public int entryY() {
        return isGas() ? 480 : 260;
    }

    /** Subir acima desta altura devolve o jogador ao Espaço. */
    public int exitY() {
        return isGas() ? 500 : 300;
    }

    public double distanceInSpace(double x, double y, double z) {
        double dx = x - cx, dy = y - cy, dz = z - cz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static Planet byDimension(ResourceKey<Level> key) {
        for (Planet p : values()) {
            if (p.dimension() == key || p.dimension().equals(key)) return p;
        }
        return null;
    }

    public static Planet byId(String id) {
        for (Planet p : values()) {
            if (p.id.equals(id)) return p;
        }
        return MOON;
    }
}
