package com.storyengine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.storyengine.StoryEngineMod;
import com.storyengine.core.Story;
import com.storyengine.dialogue.DialogueTree;
import com.storyengine.script.ScriptEngine;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public class StoryCommand {
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("story")
                .requires(s -> s.hasPermission(2))

                // /story start <id> [player]
                .then(Commands.literal("start")
                        .then(Commands.argument("story_id", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    String id = StringArgumentType.getString(ctx, "story_id");
                                    StoryEngineMod.getInstance().getStoryManager().startStory(p, id);
                                    return 1;
                                })
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                            String id = StringArgumentType.getString(ctx, "story_id");
                                            StoryEngineMod.getInstance().getStoryManager().startStory(p, id);
                                            return 1;
                                        }))))

                // /story stop [player]
                .then(Commands.literal("stop")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            StoryEngineMod.getInstance().getStoryManager().stopStory(p);
                            return 1;
                        })
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                    StoryEngineMod.getInstance().getStoryManager().stopStory(p);
                                    return 1;
                                })))

                // /story chapter <id> [player]
                .then(Commands.literal("chapter")
                        .then(Commands.argument("chapter_id", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    String id = StringArgumentType.getString(ctx, "chapter_id");
                                    StoryEngineMod.getInstance().getStoryManager().advanceToChapter(p, id);
                                    return 1;
                                })
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                            String id = StringArgumentType.getString(ctx, "chapter_id");
                                            StoryEngineMod.getInstance().getStoryManager().advanceToChapter(p, id);
                                            return 1;
                                        }))))

                // /story list
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            var stories = StoryEngineMod.getInstance().getStoryManager().getAllStories();
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("§6═══ Истории (" + stories.size() + ") ═══"), false);
                            if (stories.isEmpty()) {
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("§7  (пусто)"), false);
                            } else {
                                for (Story s : stories) {
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("  §e" + s.getId() + " §7- §f" + s.getName()
                                                    + " §7(" + s.getChapters().size() + " глав)"), false);
                                }
                            }
                            return 1;
                        }))

                // /story reload
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            StoryEngineMod.getInstance().getStoryManager().loadStories();
                            StoryEngineMod.getInstance().getDialogueManager().loadDialogues();
                            StoryEngineMod.getInstance().getQuestManager().loadQuests();
                            ScriptEngine.loadAllScripts();

                            int sc = StoryEngineMod.getInstance().getStoryManager().getAllStories().size();
                            int dc = StoryEngineMod.getInstance().getDialogueManager().getAllDialogues().size();
                            int qc = StoryEngineMod.getInstance().getQuestManager().getAllQuests().size();

                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("§aПерезагружено!"
                                            + "\n§7  Историй: §f" + sc
                                            + "\n§7  Диалогов: §f" + dc
                                            + "\n§7  Квестов: §f" + qc), true);
                            return 1;
                        }))

                // /story save
                .then(Commands.literal("save")
                        .executes(ctx -> {
                            StoryEngineMod.getInstance().getStoryManager().saveAll();
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("§aВсе истории сохранены!"), true);
                            return 1;
                        }))

                // /story create <id> <name>
                .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String id = StringArgumentType.getString(ctx, "id");
                                            String name = StringArgumentType.getString(ctx, "name");
                                            Story story = new Story(id, name);
                                            StoryEngineMod.getInstance().getStoryManager().registerStory(story);
                                            StoryEngineMod.getInstance().getStoryManager().saveStory(story);
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§aИстория создана: §e" + name), true);
                                            return 1;
                                        }))))

                // /story variable set <name> <value>
                .then(Commands.literal("variable")
                        .then(Commands.literal("set")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                                    String name = StringArgumentType.getString(ctx, "name");
                                                    String val = StringArgumentType.getString(ctx, "value");
                                                    StoryEngineMod.getInstance().getStoryManager().setVariable(p, name, val);
                                                    ctx.getSource().sendSuccess(() ->
                                                            Component.literal("§e" + name + " §a= §f" + val), false);
                                                    return 1;
                                                }))))
                        .then(Commands.literal("get")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                            String name = StringArgumentType.getString(ctx, "name");
                                            Object val = StoryEngineMod.getInstance().getStoryManager().getVariable(p, name);
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§e" + name + " §7= §f"
                                                            + (val != null ? val.toString() : "§cnull")), false);
                                            return 1;
                                        }))))

                // /story path
                .then(Commands.literal("path")
                        .executes(ctx -> {
                            var sm = StoryEngineMod.getInstance().getStoryManager();
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("§6═══ Пути StoryEngine ═══"
                                            + "\n§eИстории: §f" + sm.getStoriesFolder().toAbsolutePath()
                                            + "\n§eДиалоги: §f" + sm.getDialoguesFolder().toAbsolutePath()
                                            + "\n§eКвесты: §f" + sm.getQuestsFolder().toAbsolutePath()
                                            + "\n§eСкрипты: §f" + sm.getScriptsFolder().toAbsolutePath()), false);
                            return 1;
                        }))

                // /story status [player]
                .then(Commands.literal("status")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            return showStatus(ctx.getSource(), p);
                        })
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                    return showStatus(ctx.getSource(), p);
                                })))

                // /story debug
                .then(Commands.literal("debug")
                        .executes(ctx -> {
                            var sm = StoryEngineMod.getInstance().getStoryManager();
                            var dm = StoryEngineMod.getInstance().getDialogueManager();
                            var qm = StoryEngineMod.getInstance().getQuestManager();

                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("§6═══ Debug ═══"), false);

                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("§eИстории (" + sm.getAllStories().size() + "):"), false);
                            for (Story s : sm.getAllStories()) {
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("§7  " + s.getId() + " — "
                                                + s.getChapters().size() + " глав"), false);
                            }

                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("§eДиалоги (" + dm.getAllDialogues().size() + "):"), false);
                            for (DialogueTree t : dm.getAllDialogues()) {
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("§7  " + t.getId() + " — "
                                                + t.getNodes().size() + " узлов"), false);
                            }

                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("§eКвесты (" + qm.getAllQuests().size() + "):"), false);
                            for (var q : qm.getAllQuests()) {
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("§7  " + q.getId() + " — "
                                                + q.getObjectives().size() + " целей"), false);
                            }

                            return 1;
                        }))

                // /story editor
                .then(Commands.literal("editor")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("§aНажмите §eF7 §aдля визуального редактора"), false);
                            return 1;
                        }))
        );
    }

    private static int showStatus(CommandSourceStack source, ServerPlayer player) {
        var sm = StoryEngineMod.getInstance().getStoryManager();
        var dm = StoryEngineMod.getInstance().getDialogueManager();

        source.sendSuccess(() ->
                Component.literal("§6═══ Статус: §f" + player.getName().getString() + " §6═══"), false);

        if (sm.isPlayerInStory(player.getUUID())) {
            var state = sm.getPlayerState(player.getUUID());
            Story story = sm.getStory(state.getStoryId());
            String name = story != null ? story.getName() : state.getStoryId();
            source.sendSuccess(() ->
                    Component.literal("§eИстория: §f" + name
                            + "\n§eГлава: §f" + state.getCurrentChapter()
                            + "\n§eЗаморожен: " + (sm.isPlayerFrozen(player.getUUID()) ? "§cда" : "§aнет")), false);
        } else {
            source.sendSuccess(() -> Component.literal("§7Не в истории"), false);
        }

        source.sendSuccess(() ->
                Component.literal("§eВ диалоге: " + (dm.isInDialogue(player.getUUID()) ? "§aда" : "§7нет")), false);

        return 1;
    }
}