package com.storyengine.dialogue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.storyengine.StoryEngineMod;
import com.storyengine.network.DialoguePacket;
import com.storyengine.network.NetworkHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DialogueManager {
    private final Map<String, DialogueTree> dialogueTrees = new LinkedHashMap<>();
    private final Map<UUID, ActiveDialogue> activeDialogues = new ConcurrentHashMap<>();

    // === Регистрация ===

    public void registerDialogue(DialogueTree tree) {
        dialogueTrees.put(tree.getId(), tree);
        StoryEngineMod.LOGGER.info("Registered dialogue: {} ({} nodes)",
                tree.getId(), tree.getNodes().size());
    }

    public DialogueTree getDialogueTree(String id) {
        return dialogueTrees.get(id);
    }

    public Collection<DialogueTree> getAllDialogues() {
        return dialogueTrees.values();
    }

    // === Запуск диалога ===

    public void startDialogue(ServerPlayer player, String dialogueId) {
        DialogueTree tree = dialogueTrees.get(dialogueId);
        if (tree == null) {
            player.sendSystemMessage(Component.literal("§cДиалог '" + dialogueId + "' не найден!"));
            player.sendSystemMessage(Component.literal("§7Доступные:"));
            for (String id : dialogueTrees.keySet()) {
                player.sendSystemMessage(Component.literal("§7  - " + id));
            }
            return;
        }

        DialogueNode startNode = tree.getStartNode();
        if (startNode == null) {
            player.sendSystemMessage(Component.literal("§cДиалог не имеет начального узла!"));
            return;
        }

        ActiveDialogue active = new ActiveDialogue(player.getUUID(), dialogueId, startNode.getId());
        activeDialogues.put(player.getUUID(), active);

        StoryEngineMod.LOGGER.info("Dialogue '{}' started for {}",
                dialogueId, player.getName().getString());

        sendDialogueToClient(player, startNode);
    }

    // === Обработка выбора ===

    public void handleChoice(ServerPlayer player, int choiceIndex) {
        ActiveDialogue active = activeDialogues.get(player.getUUID());
        if (active == null) return;

        DialogueTree tree = dialogueTrees.get(active.dialogueId);
        if (tree == null) return;

        DialogueNode currentNode = tree.getNode(active.currentNodeId);
        if (currentNode == null) return;

        List<DialogueChoice> available = getAvailableChoices(player, currentNode);

        if (choiceIndex >= 0 && choiceIndex < available.size()) {
            DialogueChoice choice = available.get(choiceIndex);

            // Установить переменную
            if (choice.getSetVariable() != null && !choice.getSetVariable().isEmpty()) {
                StoryEngineMod.getInstance().getStoryManager()
                        .setVariable(player, choice.getSetVariable(), choice.getVariableValue());
            }

            // Выполнить действие выбора
            if (choice.getAction() != null && !choice.getAction().isEmpty()) {
                handleDialogueAction(player, choice.getAction());
            }

            // Перейти к узлу
            if (choice.getNextNodeId() != null && !choice.getNextNodeId().isEmpty()) {
                advanceToNode(player, active, tree, choice.getNextNodeId());
            } else {
                endDialogue(player);
            }
        }
    }

    // === Продвижение диалога (без выбора) ===

    public void advanceDialogue(ServerPlayer player) {
        ActiveDialogue active = activeDialogues.get(player.getUUID());
        if (active == null) return;

        DialogueTree tree = dialogueTrees.get(active.dialogueId);
        if (tree == null) return;

        DialogueNode currentNode = tree.getNode(active.currentNodeId);
        if (currentNode == null) return;

        // Если есть выборы — ждём
        List<DialogueChoice> available = getAvailableChoices(player, currentNode);
        if (!available.isEmpty()) return;

        // Автопереход
        if (currentNode.getNextNodeId() != null && !currentNode.getNextNodeId().isEmpty()) {
            advanceToNode(player, active, tree, currentNode.getNextNodeId());
        } else {
            endDialogue(player);
        }
    }

    // === Фильтрация выборов по условиям ===

    private List<DialogueChoice> getAvailableChoices(ServerPlayer player, DialogueNode node) {
        List<DialogueChoice> available = new ArrayList<>();
        if (node.hasChoices()) {
            for (DialogueChoice choice : node.getChoices()) {
                if (choice.getCondition() == null
                        || choice.getCondition().isEmpty()
                        || StoryEngineMod.getInstance().getStoryManager()
                        .checkCondition(player, choice.getCondition())) {
                    available.add(choice);
                }
            }
        }
        return available;
    }

    // === Переход к узлу ===

    private void advanceToNode(ServerPlayer player, ActiveDialogue active,
                               DialogueTree tree, String nodeId) {
        DialogueNode nextNode = tree.getNode(nodeId);
        if (nextNode == null) {
            StoryEngineMod.LOGGER.warn("Node '{}' not found in '{}'", nodeId, tree.getId());
            endDialogue(player);
            return;
        }
        active.currentNodeId = nodeId;
        sendDialogueToClient(player, nextNode);
    }

    // === Завершение диалога ===

    public void endDialogue(ServerPlayer player) {
        ActiveDialogue active = activeDialogues.remove(player.getUUID());

        // Закрыть экран
        NetworkHandler.sendToClient(player,
                new DialoguePacket("", "", Collections.emptyList(), true));

        // Выполнить onComplete
        if (active != null) {
            DialogueTree tree = dialogueTrees.get(active.dialogueId);
            if (tree != null && tree.getOnCompleteAction() != null
                    && !tree.getOnCompleteAction().isEmpty()) {
                StoryEngineMod.LOGGER.info("Dialogue '{}' completed, action: {}",
                        active.dialogueId, tree.getOnCompleteAction());
                handleDialogueAction(player, tree.getOnCompleteAction());
            }
        }
    }

    // === Отправка на клиент ===

    private void sendDialogueToClient(ServerPlayer player, DialogueNode node) {
        // Выборы
        List<String> choiceTexts = new ArrayList<>();
        for (DialogueChoice choice : getAvailableChoices(player, node)) {
            choiceTexts.add(choice.getText());
        }

        // Действие узла
        if (node.getAction() != null && !node.getAction().isEmpty()) {
            handleDialogueAction(player, node.getAction());
        }

        // Имя говорящего
        String speaker = (node.getSpeakerName() == null || node.getSpeakerName().isEmpty())
                ? "???" : node.getSpeakerName();

        NetworkHandler.sendToClient(player,
                new DialoguePacket(speaker, node.getText(), choiceTexts, false));
    }

    // === Обработка действий диалога ===

    private void handleDialogueAction(ServerPlayer player, String action) {
        if (action == null || action.isEmpty()) return;

        StoryEngineMod.LOGGER.info("Dialogue action: '{}'", action);

        try {
            if (action.startsWith("command:")) {
                String cmd = action.substring(8)
                        .replace("{player}", player.getName().getString());
                player.server.getCommands().performPrefixedCommand(
                        player.server.createCommandSourceStack(), cmd);

            } else if (action.startsWith("start_quest:")) {
                String questId = action.substring(12);
                StoryEngineMod.getInstance().getQuestManager()
                        .startQuest(player, questId);

            } else if (action.equals("advance_story")) {
                StoryEngineMod.getInstance().getStoryManager()
                        .processActionQueue(player);

            } else if (action.startsWith("set_var:")) {
                String varPart = action.substring(8);
                String[] parts = varPart.split("=", 2);
                if (parts.length == 2) {
                    StoryEngineMod.getInstance().getStoryManager()
                            .setVariable(player, parts[0].trim(), parts[1].trim());
                }

            } else if (action.startsWith("chapter:")) {
                String chapterId = action.substring(8).trim();
                StoryEngineMod.LOGGER.info("=== CHAPTER ADVANCE: {} ===", chapterId);
                var sm = StoryEngineMod.getInstance().getStoryManager();
                if (sm.isPlayerInStory(player.getUUID())) {
                    sm.advanceToChapter(player, chapterId);
                } else {
                    StoryEngineMod.LOGGER.warn("Player not in story!");
                }

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

            } else {
                StoryEngineMod.LOGGER.warn("Unknown dialogue action: '{}'", action);
            }
        } catch (Exception e) {
            StoryEngineMod.LOGGER.error("Error in dialogue action '{}': {}",
                    action, e.getMessage());
        }
    }

    // === Статус ===

    public boolean isInDialogue(UUID playerUUID) {
        return activeDialogues.containsKey(playerUUID);
    }

    // === Загрузка ===

    public void loadDialogues() {
        Path folder;
        if (StoryEngineMod.getInstance().getStoryManager() != null) {
            folder = StoryEngineMod.getInstance().getStoryManager().getDialoguesFolder();
        } else {
            folder = Path.of("storyengine", "dialogues");
        }

        StoryEngineMod.LOGGER.info("Loading dialogues from: {}", folder.toAbsolutePath());

        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            StoryEngineMod.LOGGER.error("Failed to create dialogues folder", e);
        }

        try {
            if (Files.exists(folder)) {
                Files.list(folder)
                        .filter(p -> p.toString().endsWith(".json"))
                        .forEach(path -> {
                            try {
                                String content = Files.readString(path);
                                JsonObject json = JsonParser.parseString(content)
                                        .getAsJsonObject();
                                DialogueTree tree = DialogueTree.fromJson(json);
                                registerDialogue(tree);
                            } catch (Exception e) {
                                StoryEngineMod.LOGGER.error("Failed to load dialogue: {}",
                                        path.getFileName());
                                e.printStackTrace();
                            }
                        });
            }
        } catch (IOException e) {
            StoryEngineMod.LOGGER.error("Failed to scan dialogues folder", e);
        }

        // Встроенный пример
        loadBuiltinDialogues();

        StoryEngineMod.LOGGER.info("Total dialogues loaded: {}", dialogueTrees.size());
    }

    private void loadBuiltinDialogues() {
        if (dialogueTrees.containsKey("example_dialogue")) return;

        DialogueTree example = new DialogueTree("example_dialogue")
                .setName("Пример")
                .addNode(new DialogueNode("start", "Старец",
                        "Приветствую, путник!")
                        .addChoice(new DialogueChoice.Builder("Привет!")
                                .next("hello").build())
                        .addChoice(new DialogueChoice.Builder("Пока.")
                                .build()))
                .addNode(new DialogueNode("hello", "Старец",
                        "Рад тебя видеть! Удачи в пути."));

        registerDialogue(example);
    }

    // === Внутренний класс ===

    private static class ActiveDialogue {
        final UUID playerUUID;
        final String dialogueId;
        String currentNodeId;

        ActiveDialogue(UUID playerUUID, String dialogueId, String startNodeId) {
            this.playerUUID = playerUUID;
            this.dialogueId = dialogueId;
            this.currentNodeId = startNodeId;
        }
    }
}