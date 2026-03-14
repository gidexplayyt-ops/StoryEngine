package com.storyengine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.storyengine.StoryEngineMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class DialogueCommand {
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("dialogue")
                .requires(s -> s.hasPermission(2))

                .then(Commands.literal("start")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    StoryEngineMod.getInstance().getDialogueManager()
                                            .startDialogue(p, StringArgumentType.getString(ctx, "id"));
                                    return 1;
                                })
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                            StoryEngineMod.getInstance().getDialogueManager()
                                                    .startDialogue(p, StringArgumentType.getString(ctx, "id"));
                                            return 1;
                                        }))))

                .then(Commands.literal("stop")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            StoryEngineMod.getInstance().getDialogueManager().endDialogue(p);
                            return 1;
                        }))

                .then(Commands.literal("list")
                        .executes(ctx -> {
                            var list = StoryEngineMod.getInstance().getDialogueManager().getAllDialogues();
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("§6Диалоги (" + list.size() + "):"), false);
                            for (var t : list) {
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("  §e" + t.getId() + " §7- §f"
                                                + t.getName() + " §7(" + t.getNodes().size() + " узлов)"), false);
                            }
                            return 1;
                        }))
        );
    }
}