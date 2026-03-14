package com.storyengine.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class StoryEngineConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // === Стамина ===
    public static final ForgeConfigSpec.BooleanValue STAMINA_ENABLED;
    public static final ForgeConfigSpec.DoubleValue STAMINA_MAX;
    public static final ForgeConfigSpec.DoubleValue STAMINA_REGEN;
    public static final ForgeConfigSpec.DoubleValue STAMINA_SPRINT_COST;
    public static final ForgeConfigSpec.DoubleValue STAMINA_JUMP_COST;
    public static final ForgeConfigSpec.DoubleValue STAMINA_ATTACK_COST;

    // === Прокачка ===
    public static final ForgeConfigSpec.BooleanValue LEVELING_ENABLED;
    public static final ForgeConfigSpec.IntValue LEVELING_BASE_XP;
    public static final ForgeConfigSpec.DoubleValue LEVELING_XP_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue LEVELING_MAX_LEVEL;
    public static final ForgeConfigSpec.IntValue LEVELING_POINTS_PER_LEVEL;

    // === Валюта ===
    public static final ForgeConfigSpec.BooleanValue CURRENCY_ENABLED;
    public static final ForgeConfigSpec.IntValue CURRENCY_START_AMOUNT;

    // === HUD ===
    public static final ForgeConfigSpec.BooleanValue HUD_QUESTS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue HUD_NOTIFICATIONS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue HUD_STAMINA_ENABLED;
    public static final ForgeConfigSpec.BooleanValue HUD_LEVEL_ENABLED;

    static {
        BUILDER.comment("StoryEngine Configuration").push("stamina");
        STAMINA_ENABLED = BUILDER
                .comment("Включить систему стамины")
                .define("enabled", true);
        STAMINA_MAX = BUILDER
                .comment("Максимальное значение стамины")
                .defineInRange("max", 100.0, 10.0, 1000.0);
        STAMINA_REGEN = BUILDER
                .comment("Регенерация стамины за тик (когда игрок стоит)")
                .defineInRange("regen", 0.5, 0.01, 10.0);
        STAMINA_SPRINT_COST = BUILDER
                .comment("Расход стамины на бег за тик")
                .defineInRange("sprintCost", 0.8, 0.01, 10.0);
        STAMINA_JUMP_COST = BUILDER
                .comment("Расход стамины на прыжок")
                .defineInRange("jumpCost", 5.0, 0.1, 50.0);
        STAMINA_ATTACK_COST = BUILDER
                .comment("Расход стамины на удар")
                .defineInRange("attackCost", 8.0, 0.1, 50.0);
        BUILDER.pop();

        BUILDER.push("leveling");
        LEVELING_ENABLED = BUILDER
                .comment("Включить систему прокачки")
                .define("enabled", true);
        LEVELING_BASE_XP = BUILDER
                .comment("Базовый XP для первого уровня")
                .defineInRange("baseXp", 100, 10, 10000);
        LEVELING_XP_MULTIPLIER = BUILDER
                .comment("Множитель XP для каждого следующего уровня")
                .defineInRange("xpMultiplier", 1.5, 1.0, 5.0);
        LEVELING_MAX_LEVEL = BUILDER
                .comment("Максимальный уровень")
                .defineInRange("maxLevel", 50, 1, 1000);
        LEVELING_POINTS_PER_LEVEL = BUILDER
                .comment("Очков навыков за каждый уровень")
                .defineInRange("pointsPerLevel", 3, 1, 20);
        BUILDER.pop();

        BUILDER.push("currency");
        CURRENCY_ENABLED = BUILDER
                .comment("Включить систему валюты (монеты)")
                .define("enabled", true);
        CURRENCY_START_AMOUNT = BUILDER
                .comment("Начальное количество монет")
                .defineInRange("startAmount", 0, 0, 100000);
        BUILDER.pop();

        BUILDER.push("hud");
        HUD_QUESTS_ENABLED = BUILDER
                .comment("Показывать активные квесты в левом верхнем углу")
                .define("questsEnabled", true);
        HUD_NOTIFICATIONS_ENABLED = BUILDER
                .comment("Показывать уведомления (монеты, XP, квесты)")
                .define("notificationsEnabled", true);
        HUD_STAMINA_ENABLED = BUILDER
                .comment("Показывать полоску стамины")
                .define("staminaEnabled", true);
        HUD_LEVEL_ENABLED = BUILDER
                .comment("Показывать уровень и XP")
                .define("levelEnabled", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "storyengine.toml");
    }
}