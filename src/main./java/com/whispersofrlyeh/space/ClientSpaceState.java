package com.whispersofrlyeh.space;

/** Estado do lado do cliente (sem classes exclusivas do cliente, seguro em servidor). */
public final class ClientSpaceState {
    /** Até quando (ms) a tela "Carregando terreno" deve ser trocada pela animação. */
    public static volatile long skipLoadingScreenUntil = 0;
    /** Animação de transição em andamento. */
    public static volatile int transitionType = -1;
    public static volatile long transitionStart = 0;

    public static void startTransition(int type) {
        transitionType = type;
        transitionStart = System.currentTimeMillis();
        skipLoadingScreenUntil = transitionStart + 4000;
    }

    private ClientSpaceState() {}
}
