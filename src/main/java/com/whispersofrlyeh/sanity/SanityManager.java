package com.whispersofrlyeh.sanity;

import com.whispersofrlyeh.network.SanitySyncPayload;
import com.whispersofrlyeh.registry.ModAttachments;
import com.whispersofrlyeh.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Ponto único para ler e alterar a sanidade. Sempre use estes métodos,
 * assim o valor fica entre 0 e 100 e o cliente é avisado da mudança.
 */
public final class SanityManager {
    public static final int MAX = 100;

    public static int get(Player player) {
        return player.getData(ModAttachments.SANITY);
    }

    public static void set(ServerPlayer player, int value) {
        int clamped = Mth.clamp(value, 0, MAX);
        if (clamped != get(player)) {
            player.setData(ModAttachments.SANITY, clamped);
            sync(player);
        }
    }

    public static void add(ServerPlayer player, int delta) {
        set(player, get(player) + delta);
    }

    /** Envia a sanidade atual para o cliente do jogador (para desenhar a HUD). */
    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SanitySyncPayload(get(player)));
    }

    /** O Elder Sign protege enquanto estiver em qualquer lugar do inventário. */
    public static boolean hasElderSign(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(ModItems.ELDER_SIGN.get())) {
                return true;
            }
        }
        return false;
    }

    private SanityManager() {}
}
