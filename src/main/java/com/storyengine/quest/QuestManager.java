package com.storyengine.quest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.storyengine.StoryEngineMod;
import com.storyengine.gui.NotificationOverlay;
import com.storyengine.gui.QuestHudOverlay;
import com.storyengine.network.NetworkHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuestManager {
    private final Map<String, Quest> questTemplates = new LinkedHashMap<>();
    private final Map<UUID, Map<String, PlayerQuestData>> playerQuests = new ConcurrentHashMap<>();

    // === Регистрация ===

    public void registerQuest(Quest quest) {
        questTemplates.put(quest.getId(), quest);
        StoryEngineMod.LOGGER.info("Registered quest: {} ({})", quest.getName(), quest.getId());
    }

    public Quest getQuestTemplate(String id) { return questTemplates.get(id); }
    public Collection<Quest> getAllQuests() { return questTemplates.values(); }

    // === Запуск квеста ===

    public void startQuest(ServerPlayer player, String questId) {
        Quest template = questTemplates.get(questId);
        if (template == null) {
            player.sendSystemMessage(Component.literal("§cКвест '" + questId + "' не найден!"));
            player.sendSystemMessage(Component.literal("§7Доступные:"));
            for (String id : questTemplates.keySet()) {
                player.sendSystemMessage(Component.literal("§7  - " + id));
            }
            return;
        }

        // Проверка предыдущего квеста
        if (template.getRequiredQuest() != null && !template.getRequiredQuest().isEmpty()) {
            PlayerQuestData req = getPlayerQuest(player.getUUID(), template.getRequiredQuest());
            if (req == null || req.state != QuestState.COMPLETED) {
                player.sendSystemMessage(Component.literal(
                        "§cНеобходимо завершить: " + template.getRequiredQuest()));
                return;
            }
        }

        // Создание данных
        PlayerQuestData data = new PlayerQuestData(questId, QuestState.ACTIVE);
        for (QuestObjective obj : template.getObjectives()) {
            data.objectives.add(new QuestObjective(
                    obj.getId(), obj.getType(), obj.getTarget(),
                    obj.getRequiredCount(), obj.getDescription()));
        }

        playerQuests.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .put(questId, data);

        // Уведомление в чат
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§6§l═══ Новый квест ═══"));
        player.sendSystemMessage(Component.literal("§e" + template.getName()));
        player.sendSystemMessage(Component.literal("§7" + template.getDescription()));
        for (QuestObjective obj : data.objectives) {
            player.sendSystemMessage(Component.literal(
                    "  §f• " + obj.getDescription()
                            + " §7[" + obj.getCurrentCount() + "/" + obj.getRequiredCount() + "]"));
        }
        player.sendSystemMessage(Component.literal("§6§l═══════════════"));

        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1.0f, 1.0f);

        // Обновить HUD
        syncQuestHud(player);
    }

    // === Убийство моба ===

    public void onEntityKilled(ServerPlayer player, String entityType) {
        Map<String, PlayerQuestData> quests = playerQuests.get(player.getUUID());
        if (quests == null) return;

        for (PlayerQuestData data : quests.values()) {
            if (data.state != QuestState.ACTIVE) continue;

            for (QuestObjective obj : data.objectives) {
                if (obj.getType() == QuestObjective.ObjectiveType.KILL_ENTITY
                        && obj.getTarget().equals(entityType)
                        && !obj.isCompleted()) {
                    obj.increment();

                    player.sendSystemMessage(Component.literal(
                            "§a✓ " + obj.getDescription() + " §7["
                                    + obj.getCurrentCount() + "/" + obj.getRequiredCount() + "]"));

                    syncQuestHud(player);
                    checkQuestCompletion(player, data.questId);
                    return;
                }
            }
        }
    }

    // === Сбор предмета ===

    public void onItemCollected(ServerPlayer player, String itemType) {
        Map<String, PlayerQuestData> quests = playerQuests.get(player.getUUID());
        if (quests == null) return;

        for (PlayerQuestData data : quests.values()) {
            if (data.state != QuestState.ACTIVE) continue;
            for (QuestObjective obj : data.objectives) {
                if (obj.getType() == QuestObjective.ObjectiveType.COLLECT_ITEM
                        && obj.getTarget().equals(itemType)
                        && !obj.isCompleted()) {
                    obj.increment();
                    syncQuestHud(player);
                    checkQuestCompletion(player, data.questId);
                    return;
                }
            }
        }
    }

    // === Достижение точки ===

    public void onReachLocation(ServerPlayer player, String locationId) {
        Map<String, PlayerQuestData> quests = playerQuests.get(player.getUUID());
        if (quests == null) return;

        for (PlayerQuestData data : quests.values()) {
            if (data.state != QuestState.ACTIVE) continue;
            for (QuestObjective obj : data.objectives) {
                if (obj.getType() == QuestObjective.ObjectiveType.REACH_LOCATION
                        && obj.getTarget().equals(locationId)
                        && !obj.isCompleted()) {
                    obj.complete();
                    player.sendSystemMessage(Component.literal("§a✓ " + obj.getDescription()));
                    syncQuestHud(player);
                    checkQuestCompletion(player, data.questId);
                    return;
                }
            }
        }
    }

    // === Проверка завершения ===

    private void checkQuestCompletion(ServerPlayer player, String questId) {
        PlayerQuestData data = getPlayerQuest(player.getUUID(), questId);
        if (data == null || data.state != QuestState.ACTIVE) return;

        Quest template = questTemplates.get(questId);
        if (template == null) return;

        if (template.isAutoComplete()
                && data.objectives.stream().allMatch(QuestObjective::isCompleted)) {
            completeQuest(player, questId);
        }
    }

    // === Завершение квеста ===

    public void completeQuest(ServerPlayer player, String questId) {
        PlayerQuestData data = getPlayerQuest(player.getUUID(), questId);
        if (data == null) return;

        data.state = QuestState.COMPLETED;

        Quest template = questTemplates.get(questId);
        if (template == null) return;

        // Награды
        for (String reward : template.getRewards()) {
            String[] parts = reward.split(":");
            if (parts.length >= 2) {
                String itemId;
                int count = 1;
                if (parts.length == 3) {
                    itemId = parts[0] + ":" + parts[1];
                    try { count = Integer.parseInt(parts[2]); }
                    catch (NumberFormatException ignored) {}
                } else {
                    itemId = parts[0] + ":" + parts[1];
                }
                var item = BuiltInRegistries.ITEM.get(new ResourceLocation(itemId));
                if (item != null) {
                    player.getInventory().add(new ItemStack(item, count));
                }
            }
        }

        // Уведомление
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal(
                "§a§l✦ Квест завершён: §e" + template.getName()));
        if (!template.getRewards().isEmpty()) {
            player.sendSystemMessage(Component.literal("§7Награды получены!"));
        }

        player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                SoundSource.MASTER, 1.0f, 1.0f);

        syncQuestHud(player);

        // Действие после завершения
        if (template.getOnCompleteAction() != null
                && !template.getOnCompleteAction().isEmpty()) {
            handleQuestAction(player, template.getOnCompleteAction());
        }
    }

    // === Действие квеста ===

    private void handleQuestAction(ServerPlayer player, String action) {
        try {
            if (action.startsWith("chapter:")) {
                String chapterId = action.substring(8).trim();
                var sm = StoryEngineMod.getInstance().getStoryManager();
                if (sm.isPlayerInStory(player.getUUID())) {
                    sm.advanceToChapter(player, chapterId);
                }
            } else if (action.startsWith("command:")) {
                String cmd = action.substring(8)
                        .replace("{player}", player.getName().getString());
                player.server.getCommands().performPrefixedCommand(
                        player.server.createCommandSourceStack(), cmd);
            } else if (action.equals("advance_story")) {
                StoryEngineMod.getInstance().getStoryManager()
                        .processActionQueue(player);
            } else if (action.startsWith("start_quest:")) {
                startQuest(player, action.substring(12));
            } else if (action.startsWith("give_coins:")) {
                try {
                    long amount = Long.parseLong(action.substring(11).trim());
                    com.storyengine.currency.CurrencyManager.addCoins(player, amount);
                } catch (NumberFormatException ignored) {}
            } else if (action.startsWith("give_xp:")) {
                try {
                    long amount = Long.parseLong(action.substring(8).trim());
                    com.storyengine.leveling.LevelingSystem.addXp(player, amount);
                } catch (NumberFormatException ignored) {}
            }
        } catch (Exception e) {
            StoryEngineMod.LOGGER.error("Quest action error: {}", e.getMessage());
        }
    }

    // === HUD синхронизация ===

    private void syncQuestHud(ServerPlayer player) {
        // Будет отправлять пакет на клиент для обновления HUD
        // Пока обновляем через чат
    }

    // === Геттеры ===

    public PlayerQuestData getPlayerQuest(UUID uuid, String questId) {
        Map<String, PlayerQuestData> quests = playerQuests.get(uuid);
        return quests != null ? quests.get(questId) : null;
    }

    public Map<String, PlayerQuestData> getPlayerQuests(UUID uuid) {
        return playerQuests.getOrDefault(uuid, Collections.emptyMap());
    }

    // === Загрузка ===

    public void loadQuests() {
        Path folder;
        if (StoryEngineMod.getInstance().getStoryManager() != null) {
            folder = StoryEngineMod.getInstance().getStoryManager().getQuestsFolder();
        } else {
            folder = Path.of("storyengine", "quests");
        }

        StoryEngineMod.LOGGER.info("Loading quests from: {}", folder.toAbsolutePath());

        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            StoryEngineMod.LOGGER.error("Failed to create quests folder", e);
        }

        try {
            if (Files.exists(folder)) {
                Files.list(folder)
                        .filter(p -> p.toString().endsWith(".json"))
                        .forEach(path -> {
                            try {
                                String content = Files.readString(path);
                                JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                                Quest quest = parseQuestFromJson(json);
                                registerQuest(quest);
                            } catch (Exception e) {
                                StoryEngineMod.LOGGER.error("Failed to load quest: {}",
                                        path.getFileName());
                                e.printStackTrace();
                            }
                        });
            }
        } catch (IOException e) {
            StoryEngineMod.LOGGER.error("Failed to scan quests folder", e);
        }

        StoryEngineMod.LOGGER.info("Total quests loaded: {}", questTemplates.size());
    }

    private Quest parseQuestFromJson(JsonObject json) {
        Quest quest = new Quest(
                json.get("id").getAsString(),
                json.get("name").getAsString());

        if (json.has("description"))
            quest.setDescription(json.get("description").getAsString());
        if (json.has("autoComplete"))
            quest.setAutoComplete(json.get("autoComplete").getAsBoolean());
        if (json.has("requiredQuest") && !json.get("requiredQuest").isJsonNull())
            quest.requireQuest(json.get("requiredQuest").getAsString());
        if (json.has("onComplete"))
            quest.onComplete(json.get("onComplete").getAsString());

        if (json.has("objectives")) {
            json.getAsJsonArray("objectives").forEach(elem -> {
                JsonObject obj = elem.getAsJsonObject();
                quest.addObjective(new QuestObjective(
                        obj.get("id").getAsString(),
                        QuestObjective.ObjectiveType.valueOf(obj.get("type").getAsString()),
                        obj.get("target").getAsString(),
                        obj.get("count").getAsInt(),
                        obj.get("description").getAsString()));
            });
        }

        if (json.has("rewards")) {
            json.getAsJsonArray("rewards").forEach(elem -> {
                String reward = elem.getAsString();
                String[] parts = reward.split(":");
                if (parts.length == 3) {
                    quest.addReward(parts[0] + ":" + parts[1], Integer.parseInt(parts[2]));
                } else if (parts.length == 2) {
                    quest.addReward(parts[0] + ":" + parts[1], 1);
                }
            });
        }

        return quest;
    }

    public void saveAll() { /* сохранение прогресса */ }

    public static class PlayerQuestData {
        public final String questId;
        public QuestState state;
        public final List<QuestObjective> objectives;

        public PlayerQuestData(String questId, QuestState state) {
            this.questId = questId;
            this.state = state;
            this.objectives = new ArrayList<>();
        }
    }
}