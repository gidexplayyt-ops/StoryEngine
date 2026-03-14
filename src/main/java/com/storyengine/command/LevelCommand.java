package com.storyengine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.storyengine.leveling.LevelingSystem;
import com.storyengine.leveling.PlayerStats;
import com.storyengine.leveling.Skill;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class LevelCommand {
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("level")
                .requires(s -> s.hasPermission(2))

                // /level addxp <amount> [player]
                .then(Commands.literal("addxp")
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    long amount = LongArgumentType.getLong(ctx, "amount");
                                    LevelingSystem.addXp(p, amount);
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("§b+" + amount + " XP"), false);
                                    return 1;
                                })
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                            long amount = LongArgumentType.getLong(ctx, "amount");
                                            LevelingSystem.addXp(p, amount);
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§b+" + amount + " XP §7для §f"
                                                            + p.getName().getString()), false);
                                            return 1;
                                        }))))

                // /level upgrade <skill>
                .then(Commands.literal("upgrade")
                        .then(Commands.argument("skill", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    String skillName = StringArgumentType.getString(ctx, "skill").toUpperCase();
                                    try {
                                        Skill skill = Skill.valueOf(skillName);
                                        if (LevelingSystem.upgradeSkill(p, skill)) {
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§a" + skill.displayName + " улучшен!"), false);
                                        } else {
                                            ctx.getSource().sendFailure(
                                                    Component.literal("§cНет очков навыков!"));
                                        }
                                    } catch (IllegalArgumentException e) {
                                        ctx.getSource().sendFailure(Component.literal(
                                                "§cНавык не найден! Доступные: strength, vitality, agility, endurance, luck"));
                                    }
                                    return 1;
                                })))

                // /level info [player]
                .then(Commands.literal("info")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            return showInfo(ctx.getSource(), p);
                        })
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                    return showInfo(ctx.getSource(), p);
                                })))

                // /level reset [player]
                .then(Commands.literal("reset")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            LevelingSystem.remove(p.getUUID());
                            LevelingSystem.syncToClient(p);
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("§cПрокачка сброшена!"), true);
                            return 1;
                        }))
        );
    }

    private static int showInfo(CommandSourceStack src, ServerPlayer p) {
        PlayerStats s = LevelingSystem.getStats(p.getUUID());
        src.sendSuccess(() ->
                Component.literal("§6═══ " + p.getName().getString() + " ═══"
                        + "\n§7Уровень: §f" + s.getLevel()
                        + "\n§7XP: §f" + s.getXp() + "/" + s.getXpToNextLevel()
                        + " §7(" + String.format("%.0f", s.getXpPercentage() * 100) + "%)"
                        + "\n§7Очки: §e" + s.getAvailablePoints()
                        + "\n§c⚔ Сила: " + s.getSkillLevel(Skill.STRENGTH)
                        + " §7(+" + String.format("%.1f", s.getStrengthBonus()) + " урон)"
                        + "\n§a❤ HP: " + s.getSkillLevel(Skill.VITALITY)
                        + " §7(+" + String.format("%.0f", s.getHealthBonus()) + " HP)"
                        + "\n§b⚡ Ловкость: " + s.getSkillLevel(Skill.AGILITY)
                        + " §7(+" + String.format("%.0f", s.getSpeedBonus() * 100) + "% скорость)"
                        + "\n§e◆ Выносл: " + s.getSkillLevel(Skill.ENDURANCE)
                        + " §7(+" + String.format("%.0f", s.getStaminaBonus()) + " стамина)"
                        + "\n§d★ Удача: " + s.getSkillLevel(Skill.LUCK)
                        + " §7(+" + String.format("%.0f", s.getLuckBonus()) + ")"
                        + "\n§7Система: " + (LevelingSystem.isEnabled() ? "§aвкл" : "§cвыкл")), false);
        return 1;
    }
}