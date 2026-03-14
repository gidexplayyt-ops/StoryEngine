package com.storyengine.leveling;

import com.storyengine.config.StoryEngineConfig;
import com.storyengine.network.LevelSyncPacket;
import com.storyengine.network.NetworkHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LevelingSystem {
    private static final Map<UUID, PlayerStats> playerStats = new ConcurrentHashMap<>();

    private static final UUID HEALTH_MOD_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID SPEED_MOD_ID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");
    private static final UUID DAMAGE_MOD_ID = UUID.fromString("c3d4e5f6-a7b8-9012-cdef-123456789012");

    public static boolean isEnabled() {
        return StoryEngineConfig.LEVELING_ENABLED.get();
    }

    public static PlayerStats getStats(UUID uuid) {
        return playerStats.computeIfAbsent(uuid, k -> new PlayerStats());
    }

    public static void addXp(ServerPlayer player, long amount) {
        if (!isEnabled()) return;

        PlayerStats stats = getStats(player.getUUID());
        boolean leveled = stats.addXp(amount,
                StoryEngineConfig.LEVELING_MAX_LEVEL.get(),
                StoryEngineConfig.LEVELING_BASE_XP.get(),
                StoryEngineConfig.LEVELING_XP_MULTIPLIER.get(),
                StoryEngineConfig.LEVELING_POINTS_PER_LEVEL.get());

        if (leveled) {
            player.sendSystemMessage(Component.literal(
                    "§6§l⬆ УРОВЕНЬ " + stats.getLevel() + "! §e+"
                            + StoryEngineConfig.LEVELING_POINTS_PER_LEVEL.get() + " очков"));
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1.0f, 1.2f);
            applyStatBonuses(player);
        }

        syncToClient(player);
    }

    public static boolean upgradeSkill(ServerPlayer player, Skill skill) {
        if (!isEnabled()) return false;

        PlayerStats stats = getStats(player.getUUID());
        if (stats.upgradeSkill(skill)) {
            player.sendSystemMessage(Component.literal(
                    skill.color + "§l⬆ " + skill.displayName + " §fур. "
                            + stats.getSkillLevel(skill)));
            applyStatBonuses(player);
            syncToClient(player);
            return true;
        }
        return false;
    }

    public static void applyStatBonuses(ServerPlayer player) {
        PlayerStats stats = getStats(player.getUUID());

        applyModifier(player, Attributes.MAX_HEALTH, HEALTH_MOD_ID,
                "se.health", stats.getHealthBonus(), AttributeModifier.Operation.ADDITION);
        applyModifier(player, Attributes.MOVEMENT_SPEED, SPEED_MOD_ID,
                "se.speed", stats.getSpeedBonus(), AttributeModifier.Operation.MULTIPLY_BASE);
        applyModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_MOD_ID,
                "se.damage", stats.getStrengthBonus(), AttributeModifier.Operation.ADDITION);
    }

    private static void applyModifier(ServerPlayer player,
                                      net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                      UUID modId, String name, double value,
                                      AttributeModifier.Operation op) {
        var attr = player.getAttribute(attribute);
        if (attr != null) {
            attr.removeModifier(modId);
            if (value > 0) {
                attr.addPermanentModifier(new AttributeModifier(modId, name, value, op));
            }
        }
    }

    public static void syncToClient(ServerPlayer player) {
        PlayerStats stats = getStats(player.getUUID());
        NetworkHandler.sendToClient(player, new LevelSyncPacket(
                stats.getLevel(), stats.getXp(), stats.getXpToNextLevel(),
                stats.getAvailablePoints(),
                stats.getSkillLevel(Skill.STRENGTH),
                stats.getSkillLevel(Skill.VITALITY),
                stats.getSkillLevel(Skill.AGILITY),
                stats.getSkillLevel(Skill.ENDURANCE),
                stats.getSkillLevel(Skill.LUCK)));
    }

    public static void remove(UUID uuid) {
        playerStats.remove(uuid);
    }
}