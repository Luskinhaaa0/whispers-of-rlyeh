package com.whispersofrlyeh.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.whispersofrlyeh.sanity.SanityManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comandos para testar:
 *   /sanity get
 *   /sanity set <0-100>
 * Precisa de cheats ativados (ou ser operador no servidor).
 */
public final class SanityCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sanity")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("get")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int value = SanityManager.get(player);
                            ctx.getSource().sendSuccess(
                                    () -> Component.translatable("commands.whispersofrlyeh.sanity.get", value), false);
                            return value;
                        }))
                .then(Commands.literal("set")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, SanityManager.MAX))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int value = IntegerArgumentType.getInteger(ctx, "value");
                                    SanityManager.set(player, value);
                                    ctx.getSource().sendSuccess(
                                            () -> Component.translatable("commands.whispersofrlyeh.sanity.set", value), true);
                                    return value;
                                }))));
    }

    private SanityCommand() {}
}
