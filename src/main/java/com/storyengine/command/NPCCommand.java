package com.storyengine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.storyengine.StoryEngineMod;
import com.storyengine.entity.StoryNPC;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public class NPCCommand {
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("npc")
                .requires(s -> s.hasPermission(2))

                // /npc spawn <id> <name> [pos] [skin]
                .then(Commands.literal("spawn")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .executes(ctx -> {
                                            Vec3 pos = ctx.getSource().getPosition();
                                            return spawn(ctx.getSource(),
                                                    StringArgumentType.getString(ctx, "id"),
                                                    StringArgumentType.getString(ctx, "name"),
                                                    pos, "");
                                        })
                                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                                .executes(ctx -> {
                                                    Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
                                                    return spawn(ctx.getSource(),
                                                            StringArgumentType.getString(ctx, "id"),
                                                            StringArgumentType.getString(ctx, "name"),
                                                            pos, "");
                                                })
                                                .then(Commands.argument("skin", StringArgumentType.word())
                                                        .executes(ctx -> {
                                                            Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
                                                            return spawn(ctx.getSource(),
                                                                    StringArgumentType.getString(ctx, "id"),
                                                                    StringArgumentType.getString(ctx, "name"),
                                                                    pos,
                                                                    StringArgumentType.getString(ctx, "skin"));
                                                        }))))))

                // /npc remove <id>
                .then(Commands.literal("remove")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "id");
                                    StoryEngineMod.getInstance().getNpcManager().removeNPC(id);
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("§cNPC удалён: §e" + id), true);
                                    return 1;
                                })))

                // /npc removeall
                .then(Commands.literal("removeall")
                        .executes(ctx -> {
                            StoryEngineMod.getInstance().getNpcManager().removeAllNPCs();
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("§cВсе NPC удалены!"), true);
                            return 1;
                        }))

                // /npc move <id> <pos> [speed]
                .then(Commands.literal("move")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(ctx -> {
                                            String id = StringArgumentType.getString(ctx, "id");
                                            Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
                                            StoryEngineMod.getInstance().getNpcManager()
                                                    .moveNPC(id, pos.x, pos.y, pos.z, 1.0f);
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§aNPC §e" + id + " §aидёт к §f" +
                                                            String.format("%.0f %.0f %.0f", pos.x, pos.y, pos.z)), false);
                                            return 1;
                                        })
                                        .then(Commands.argument("speed", FloatArgumentType.floatArg(0.1f, 5.0f))
                                                .executes(ctx -> {
                                                    String id = StringArgumentType.getString(ctx, "id");
                                                    Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
                                                    float speed = FloatArgumentType.getFloat(ctx, "speed");
                                                    StoryEngineMod.getInstance().getNpcManager()
                                                            .moveNPC(id, pos.x, pos.y, pos.z, speed);
                                                    return 1;
                                                })))))

                // /npc dialogue <id> <dialogue_id>
                .then(Commands.literal("dialogue")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("dialogue_id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String id = StringArgumentType.getString(ctx, "id");
                                            String did = StringArgumentType.getString(ctx, "dialogue_id");
                                            StoryEngineMod.getInstance().getNpcManager().setNPCDialogue(id, did);
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§aDiалог NPC §e" + id + " §a= §f" + did), false);
                                            return 1;
                                        }))))

                // /npc list
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            var npcs = StoryEngineMod.getInstance().getNpcManager().getAllNPCs();
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("§6NPC (" + npcs.size() + "):"), false);
                            for (var entry : npcs.entrySet()) {
                                StoryNPC npc = entry.getValue();
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("  §e" + entry.getKey() + " §7- §f"
                                                + npc.getNPCDisplayName() + " §7at §f"
                                                + String.format("%.0f, %.0f, %.0f",
                                                npc.getX(), npc.getY(), npc.getZ())
                                                + (npc.getDialogueId().isEmpty() ? "" :
                                                " §7dial=§b" + npc.getDialogueId())), false);
                            }
                            return 1;
                        }))
        );
    }

    private static int spawn(CommandSourceStack src, String id, String name,
                             Vec3 pos, String skin) {
        ServerLevel level = src.getLevel();
        StoryNPC npc = StoryEngineMod.getInstance().getNpcManager()
                .spawnNPC(level, id, name, pos.x, pos.y, pos.z, skin);
        if (npc != null) {
            src.sendSuccess(() ->
                    Component.literal("§aNPC: §e" + name + " §7(" + id + ")"), true);
        } else {
            src.sendFailure(Component.literal("§cОшибка создания NPC!"));
        }
        return 1;
    }
}