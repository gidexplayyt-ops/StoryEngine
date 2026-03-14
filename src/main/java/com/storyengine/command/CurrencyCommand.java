package com.storyengine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.storyengine.currency.CurrencyManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class CurrencyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("coins")
                .requires(s -> s.hasPermission(2))

                // /coins add <amount> [player]
                .then(Commands.literal("add")
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    long amount = LongArgumentType.getLong(ctx, "amount");
                                    CurrencyManager.addCoins(p, amount);
                                    return 1;
                                })
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                            long amount = LongArgumentType.getLong(ctx, "amount");
                                            CurrencyManager.addCoins(p, amount);
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§6+" + amount + " монет §7для §f"
                                                            + p.getName().getString()), false);
                                            return 1;
                                        }))))

                // /coins remove <amount> [player]
                .then(Commands.literal("remove")
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    long amount = LongArgumentType.getLong(ctx, "amount");
                                    CurrencyManager.removeCoins(p, amount);
                                    return 1;
                                })
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                            long amount = LongArgumentType.getLong(ctx, "amount");
                                            CurrencyManager.removeCoins(p, amount);
                                            return 1;
                                        }))))

                // /coins balance [player]
                .then(Commands.literal("balance")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            long bal = CurrencyManager.getBalance(p.getUUID());
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("§6Баланс: §e" + bal + " монет"), false);
                            return 1;
                        })
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                    long bal = CurrencyManager.getBalance(p.getUUID());
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("§6Баланс " + p.getName().getString()
                                                    + ": §e" + bal + " монет"), false);
                                    return 1;
                                })))

                // /coins set <amount> [player]
                .then(Commands.literal("set")
                        .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    long amount = LongArgumentType.getLong(ctx, "amount");
                                    CurrencyManager.setBalance(p.getUUID(), amount);
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("§6Баланс установлен: §e" + amount), false);
                                    return 1;
                                })
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                            long amount = LongArgumentType.getLong(ctx, "amount");
                                            CurrencyManager.setBalance(p.getUUID(), amount);
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§6Баланс " + p.getName().getString()
                                                            + " = §e" + amount), false);
                                            return 1;
                                        }))))
        );
    }
}