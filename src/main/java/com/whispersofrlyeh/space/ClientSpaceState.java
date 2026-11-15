package com.whispersofrlyeh.space;

/** Estado do lado do cliente (sem classes exclusivas do cliente, seguro em servidor). */
public final class ClientSpaceState {
    /** Até quando (System.currentTimeMillis) a tela "Carregando terreno" deve ser pulada. */
    public static volatile long skipLoadingScreenUntil = 0;

    private ClientSpaceState() {}
}
