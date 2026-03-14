package com.storyengine.stamina;

import com.storyengine.config.StoryEngineConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StaminaSystem {
    private static final Map<UUID, StaminaData> playerStamina = new ConcurrentHashMap<>();

    public static boolean isEnabled() {
        return StoryEngineConfig.STAMINA_ENABLED.get();
    }

    public static StaminaData getData(UUID uuid) {
        return playerStamina.computeIfAbsent(uuid, k -> {
            StaminaData data = new StaminaData();
            data.setMaxStamina(StoryEngineConfig.STAMINA_MAX.get());
            return data;
        });
    }

    public static boolean consumeSprint(UUID uuid) {
        if (!isEnabled()) return true;
        return getData(uuid).consume(StoryEngineConfig.STAMINA_SPRINT_COST.get());
    }

    public static boolean consumeJump(UUID uuid) {
        if (!isEnabled()) return true;
        return getData(uuid).consume(StoryEngineConfig.STAMINA_JUMP_COST.get());
    }

    public static boolean consumeAttack(UUID uuid) {
        if (!isEnabled()) return true;
        return getData(uuid).consume(StoryEngineConfig.STAMINA_ATTACK_COST.get());
    }

    public static void tickRegeneration(UUID uuid) {
        if (!isEnabled()) return;
        getData(uuid).regenerate(StoryEngineConfig.STAMINA_REGEN.get());
    }

    public static void fill(UUID uuid) {
        getData(uuid).fill();
    }

    public static void remove(UUID uuid) {
        playerStamina.remove(uuid);
    }
}