package com.storyengine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.storyengine.stamina.StaminaData;
import com.storyengine.stamina.StaminaSystem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class StaminaCommand {
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("stamina")
                .requires(s -> s.hasPermission(2))

                // /stamina fill [player]
                .then(Commands.literal("fill")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            StaminaSystem.fill(p.getUUID());
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("§aСтамина восполнена"), false);
                            return 1;
                        })
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                    StaminaSystem.fill(p.getUUID());
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("§aСтамина " + p.getName().getString() + " восполнена"), false);
                                    return 1;
                                })))

                // /stamina set <amount>
                .then(Commands.literal("set")
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    double amount = DoubleArgumentType.getDouble(ctx, "amount");
                                    StaminaData data = StaminaSystem.getData(p.getUUID());
                                    data.fill();
                                    double excess = data.getMaxStamina() - amount;
                                    if (excess > 0) data.consume(excess);
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("§aСтамина: §f" + (int) amount), false);
                                    return 1;
                                })))

                // /stamina info
                .then(Commands.literal("info")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            StaminaData data = StaminaSystem.getData(p.getUUID());
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("§6Стамина:"
                                            + "\n§7  Текущая: §f" + String.format("%.1f", data.getStamina())
                                            + "\n§7  Макс: §f" + String.format("%.1f", data.getMaxStamina())
                                            + "\n§7  Истощён: " + (data.isExhausted() ? "§cда" : "§aнет")
                                            + "\n§7  Система: " + (StaminaSystem.isEnabled() ? "§aвкл" : "§cвыкл")), false);
                            return 1;
                        }))
        );
    }
}