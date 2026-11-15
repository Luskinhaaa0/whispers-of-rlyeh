package com.whispersofrlyeh.registry;

import com.mojang.serialization.Codec;
import com.whispersofrlyeh.WhispersOfRlyeh;
import com.whispersofrlyeh.sanity.SanityManager;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * "Attachments" são dados extras que o NeoForge salva junto com entidades.
 * Aqui guardamos a sanidade do jogador (0 a 100). Ela é salva no mundo
 * e volta para 100 quando o jogador morre.
 */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, WhispersOfRlyeh.MODID);

    public static final Supplier<AttachmentType<Integer>> SANITY = ATTACHMENT_TYPES.register("sanity",
            () -> AttachmentType.builder(() -> SanityManager.MAX).serialize(Codec.INT).build());

    private ModAttachments() {}
}
