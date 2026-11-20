package com.whispersofrlyeh.sanity;

/**
 * Cópia da sanidade do lado do cliente, usada só para desenhar a HUD.
 * Esta classe não usa nenhuma classe exclusiva do cliente, então é segura em servidores.
 */
public final class ClientSanityData {
    private static volatile int sanity = SanityManager.MAX;

    public static int get() {
        return sanity;
    }

    public static void set(int value) {
        sanity = value;
    }

    private ClientSanityData() {}
}
