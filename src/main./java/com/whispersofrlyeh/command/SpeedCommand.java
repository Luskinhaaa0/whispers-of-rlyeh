package com.whispersofrlyeh.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.whispersofrlyeh.WhispersOfRlyeh;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * /speed set <número 0.1–50>     multiplicador livre
 * /speed set <nível>             níveis estilo powerscaling
 * /speed get                     mostra a velocidade atual
 * /speed reset                   volta ao normal
 *
 * Afeta: andar/correr, voo do criativo e o impulso no Espaço.
 * Acima de 2x também aumenta a altura de passo (sobe 1 bloco sem pular).
 * Precisa de cheats (nível de permissão 2).
 */
public final class SpeedCommand {

    public static final ResourceLocation SPEED_ID = WhispersOfRlyeh.id("speed_command");
    private static final ResourceLocation STEP_ID = WhispersOfRlyeh.id("speed_command_step");
    private static final float BASE_FLY_SPEED = 0.05f;

    /** Níveis de poder, do mais lento ao mais rápido. */
    public static final Map<String, Double> TIERS = new LinkedHashMap<>();
    static {
        TIERS.put("tartaruga", 0.25);
        TIERS.put("lento", 0.5);
        TIERS.put("humano", 1.0);
        TIERS.put("atleta", 1.5);
        TIERS.put("velocista", 2.5);
        TIERS.put("supersonico", 5.0);
        TIERS.put("hipersonico", 10.0);
        TIERS.put("relampago", 20.0);
        TIERS.put("luz", 50.0);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> set = Commands.literal("set")
                .then(Commands.argument("multiplicador", DoubleArgumentType.doubleArg(0.1, 50.0))
                        .executes(ctx -> apply(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "multiplicador"), null)));
        for (Map.Entry<String, Double> tier : TIERS.entrySet()) {
            set.then(Commands.literal(tier.getKey())
                    .executes(ctx -> apply(ctx.getSource(), tier.getValue(), tier.getKey())));
        }

        dispatcher.register(Commands.literal("speed")
                .requires(source -> source.hasPermission(2))
                .then(set)
                .then(Commands.literal("reset")
                        .executes(ctx -> apply(ctx.getSource(), 1.0, "humano")))
                .then(Commands.literal("get")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            double m = getMultiplier(player);
                            ctx.getSource().sendSuccess(() -> Component.translatable(
                                    "commands.whispersofrlyeh.speed.get", describe(m)), false);
                            return (int) Math.round(m * 100);
                        })));
    }

    private static int apply(CommandSourceStack source, double multiplier, String tierName) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        // Andar / correr
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_ID);
            if (multiplier != 1.0) {
                speed.addPermanentModifier(new AttributeModifier(SPEED_ID, multiplier - 1.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }

        // Altura de passo: rápido demais e você tropeça em todo bloco
        AttributeInstance step = player.getAttribute(Attributes.STEP_HEIGHT);
        if (step != null) {
            step.removeModifier(STEP_ID);
            if (multiplier >= 2.0) {
                step.addPermanentModifier(new AttributeModifier(STEP_ID, 0.5, AttributeModifier.Operation.ADD_VALUE));
            }
        }

        // Voo do criativo
        player.getAbilities().setFlyingSpeed((float) Math.min(1.0, BASE_FLY_SPEED * multiplier));
        player.onUpdateAbilities();

        String label = tierName != null ? describeTier(tierName, multiplier) : describe(multiplier);
        source.sendSuccess(() -> Component.translatable("commands.whispersofrlyeh.speed.set", label)
                .withStyle(ChatFormatting.AQUA), false);
        return (int) Math.round(multiplier * 100);
    }

    /** Multiplicador atual do jogador (1.0 = normal). Também usado no cliente para o impulso no Espaço. */
    public static double getMultiplier(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return 1.0;
        AttributeModifier mod = speed.getModifier(SPEED_ID);
        return mod == null ? 1.0 : 1.0 + mod.amount();
    }

    private static String describe(double m) {
        for (Map.Entry<String, Double> tier : TIERS.entrySet()) {
            if (Math.abs(tier.getValue() - m) < 0.001) return describeTier(tier.getKey(), m);
        }
        return String.format(Locale.ROOT, "%.2fx", m);
    }

    private static String describeTier(String name, double m) {
        String pretty = switch (name) {
            case "supersonico" -> "Supersônico";
            case "hipersonico" -> "Hipersônico";
            case "relampago" -> "Relâmpago";
            case "luz" -> "Velocidade da Luz";
            default -> Character.toUpperCase(name.charAt(0)) + name.substring(1);
        };
        return pretty + " (" + String.format(Locale.ROOT, "%.2f", m).replaceAll("\\.?0+$", "") + "x)";
    }

    private SpeedCommand() {}
}
