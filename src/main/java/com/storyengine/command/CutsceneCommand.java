package com.storyengine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.storyengine.StoryEngineMod;
import com.storyengine.cutscene.CameraPath;
import com.storyengine.cutscene.Cutscene;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CutsceneCommand {
    private static final Map<UUID, CutsceneBuilder> builders = new HashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("cutscene")
                .requires(s -> s.hasPermission(2))

                .then(Commands.literal("play")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    StoryEngineMod.getInstance().getCutsceneManager()
                                            .startCutscene(p, StringArgumentType.getString(ctx, "id"));
                                    return 1;
                                })
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                            StoryEngineMod.getInstance().getCutsceneManager()
                                                    .startCutscene(p, StringArgumentType.getString(ctx, "id"));
                                            return 1;
                                        }))))

                .then(Commands.literal("stop")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            StoryEngineMod.getInstance().getCutsceneManager().stopCutscene(p);
                            return 1;
                        }))

                .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                            String id = StringArgumentType.getString(ctx, "id");
                                            String name = StringArgumentType.getString(ctx, "name");
                                            builders.put(p.getUUID(), new CutsceneBuilder(id, name));
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§aСоздание: §e" + name
                                                            + "\n§7/cutscene addpoint [ticks]"
                                                            + "\n§7/cutscene finish"), true);
                                            return 1;
                                        }))))

                .then(Commands.literal("addpoint")
                        .executes(ctx -> addPoint(ctx.getSource(), 60))
                        .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                .executes(ctx -> addPoint(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "duration")))))

                .then(Commands.literal("finish")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            CutsceneBuilder b = builders.remove(p.getUUID());
                            if (b == null) {
                                ctx.getSource().sendFailure(
                                        Component.literal("§cНет активного создания!"));
                                return 0;
                            }
                            Cutscene cs = b.build();
                            StoryEngineMod.getInstance().getCutsceneManager().registerCutscene(cs);
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("§aКатсцена §e" + cs.getName()
                                            + " §aсохранена! (" + b.count + " точек)"), true);
                            return 1;
                        }))

                .then(Commands.literal("list")
                        .executes(ctx -> {
                            var list = StoryEngineMod.getInstance().getCutsceneManager().getAllCutscenes();
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("§6Катсцены (" + list.size() + "):"), false);
                            for (var cs : list) {
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("  §e" + cs.getId() + " §7- §f"
                                                + cs.getName() + " §7(" + cs.getDuration() + " тиков)"), false);
                            }
                            return 1;
                        }))
        );
    }

    private static int addPoint(CommandSourceStack src, int duration) {
        try {
            ServerPlayer p = src.getPlayerOrException();
            CutsceneBuilder b = builders.get(p.getUUID());
            if (b == null) {
                src.sendFailure(Component.literal("§cСначала /cutscene create <id> <name>"));
                return 0;
            }
            b.addPoint(p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot(), duration);
            src.sendSuccess(() ->
                    Component.literal("§aТочка #" + b.count + " §7("
                            + String.format("%.0f %.0f %.0f", p.getX(), p.getY(), p.getZ())
                            + ", " + duration + "t)"), false);
            return 1;
        } catch (Exception e) { return 0; }
    }

    private static class CutsceneBuilder {
        String id, name;
        CameraPath path = new CameraPath();
        int count = 0;

        CutsceneBuilder(String id, String name) { this.id = id; this.name = name; }

        void addPoint(double x, double y, double z, float yaw, float pitch, int dur) {
            path.addPoint(x, y, z, yaw, pitch, dur);
            count++;
        }

        Cutscene build() { return new Cutscene(id).setName(name).setCameraPath(path); }
    }
}