package com.storyengine.core;

import com.google.gson.*;
import com.storyengine.StoryEngineMod;
import com.storyengine.currency.CurrencyManager;
import com.storyengine.entity.StoryNPC;
import com.storyengine.leveling.LevelingSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StoryManager {
    private final MinecraftServer server;
    private final Map<String, Story> stories = new LinkedHashMap<>();
    private final Map<UUID, PlayerStoryState> playerStates = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Object>> playerVariables = new ConcurrentHashMap<>();
    private final Map<UUID, Queue<ScheduledAction>> actionQueues = new ConcurrentHashMap<>();
    private final Set<UUID> frozenPlayers = ConcurrentHashMap.newKeySet();

    private final Path storiesFolder;
    private final Path dialoguesFolder;
    private final Path questsFolder;
    private final Path scriptsFolder;

    public StoryManager(MinecraftServer server) {
        this.server = server;
        Path base = server.getServerDirectory().toPath().resolve("storyengine");
        this.storiesFolder = base.resolve("stories");
        this.dialoguesFolder = base.resolve("dialogues");
        this.questsFolder = base.resolve("quests");
        this.scriptsFolder = base.resolve("scripts");

        try {
            Files.createDirectories(storiesFolder);
            Files.createDirectories(dialoguesFolder);
            Files.createDirectories(questsFolder);
            Files.createDirectories(scriptsFolder);
        } catch (IOException e) {
            StoryEngineMod.LOGGER.error("Failed to create StoryEngine folders", e);
        }
    }

    // === Пути ===

    public Path getStoriesFolder() { return storiesFolder; }
    public Path getDialoguesFolder() { return dialoguesFolder; }
    public Path getQuestsFolder() { return questsFolder; }
    public Path getScriptsFolder() { return scriptsFolder; }

    // === Управление историями ===

    public void registerStory(Story story) {
        stories.put(story.getId(), story);
        StoryEngineMod.LOGGER.info("Registered story: {} ({})", story.getName(), story.getId());
    }

    public Story getStory(String id) { return stories.get(id); }
    public Collection<Story> getAllStories() { return stories.values(); }
    public void removeStory(String id) { stories.remove(id); }

    // === Запуск/остановка ===

    public void startStory(ServerPlayer player, String storyId) {
        Story story = stories.get(storyId);
        if (story == null) {
            player.sendSystemMessage(Component.literal("§cИстория '" + storyId + "' не найдена!"));
            player.sendSystemMessage(Component.literal("§7Доступные: " + String.join(", ", stories.keySet())));
            player.sendSystemMessage(Component.literal("§7Папка: §f" + storiesFolder.toAbsolutePath()));
            return;
        }

        PlayerStoryState state = new PlayerStoryState(player.getUUID(), storyId);
        state.setCurrentChapter(story.getStartChapterId());
        state.setCurrentActionIndex(0);
        playerStates.put(player.getUUID(), state);
        playerVariables.putIfAbsent(player.getUUID(), new HashMap<>());

        player.sendSystemMessage(Component.literal("§a§l▶ История: §e" + story.getName()));
        executeChapter(player, story, story.getStartChapter());
    }

    public void stopStory(ServerPlayer player) {
        PlayerStoryState state = playerStates.remove(player.getUUID());
        actionQueues.remove(player.getUUID());
        frozenPlayers.remove(player.getUUID());
        if (state != null) {
            player.sendSystemMessage(Component.literal("§c§l■ История остановлена"));
        }
    }

    public void advanceToChapter(ServerPlayer player, String chapterId) {
        PlayerStoryState state = playerStates.get(player.getUUID());
        if (state == null) return;

        Story story = stories.get(state.getStoryId());
        if (story == null) return;

        StoryChapter chapter = story.getChapter(chapterId);
        if (chapter == null) {
            player.sendSystemMessage(Component.literal("§cГлава '" + chapterId + "' не найдена!"));
            return;
        }

        actionQueues.remove(player.getUUID());
        state.setCurrentChapter(chapterId);
        state.setCurrentActionIndex(0);
        executeChapter(player, story, chapter);
    }

    // === Выполнение главы ===

    private void executeChapter(ServerPlayer player, Story story, StoryChapter chapter) {
        if (chapter == null) return;

        player.sendSystemMessage(Component.literal("§6§l◆ Глава: §f" + chapter.getName()));

        Queue<ScheduledAction> queue = new LinkedList<>();
        int totalDelay = 0;
        for (StoryAction action : chapter.getActions()) {
            totalDelay += action.getDelay();
            queue.add(new ScheduledAction(action, totalDelay));
        }

        actionQueues.put(player.getUUID(), queue);
        processActionQueue(player);
    }

    public void processActionQueue(ServerPlayer player) {
        Queue<ScheduledAction> queue = actionQueues.get(player.getUUID());
        if (queue == null || queue.isEmpty()) {
            onChapterComplete(player);
            return;
        }

        ScheduledAction scheduled = queue.peek();
        if (scheduled != null && scheduled.delay <= 0) {
            queue.poll();
            executeAction(player, scheduled.action);
            if (!queue.isEmpty() && queue.peek().delay <= 0) {
                processActionQueue(player);
            }
        }
    }

    public void tick() {
        // Очередь действий
        for (Map.Entry<UUID, Queue<ScheduledAction>> entry : actionQueues.entrySet()) {
            Queue<ScheduledAction> queue = entry.getValue();
            if (queue == null || queue.isEmpty()) continue;

            ScheduledAction first = queue.peek();
            if (first != null && first.delay > 0) {
                first.delay--;
                if (first.delay <= 0) {
                    ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                    if (player != null) processActionQueue(player);
                }
            }
        }

        // Заморозка
        for (UUID uuid : frozenPlayers) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                PlayerStoryState state = playerStates.get(uuid);
                if (state != null && state.getFreezePos() != null) {
                    double[] pos = state.getFreezePos();
                    player.teleportTo(pos[0], pos[1], pos[2]);
                }
            }
        }
    }

    private void onChapterComplete(ServerPlayer player) {
        PlayerStoryState state = playerStates.get(player.getUUID());
        if (state == null) return;

        Story story = stories.get(state.getStoryId());
        if (story == null) return;

        StoryChapter current = story.getChapter(state.getCurrentChapter());
        if (current != null && current.isAutoAdvance()) {
            String nextId = story.getNextChapterId(state.getCurrentChapter());
            if (nextId != null) {
                state.setCurrentChapter(nextId);
                state.setCurrentActionIndex(0);
                executeChapter(player, story, story.getChapter(nextId));
            } else {
                player.sendSystemMessage(Component.literal("§a§l✦ История завершена: §e" + story.getName()));
                playerStates.remove(player.getUUID());
            }
        }
    }

    // === Выполнение действий ===

    public void executeAction(ServerPlayer player, StoryAction action) {
        if (action == null) return;

        if (action.getCondition() != null && !action.getCondition().isEmpty()) {
            if (!checkCondition(player, action.getCondition())) return;
        }

        ServerLevel level = player.serverLevel();

        try {
            switch (action.getType()) {
                case DIALOGUE -> {
                    String dialogueId = action.getString("dialogue_id");
                    StoryEngineMod.getInstance().getDialogueManager().startDialogue(player, dialogueId);
                }

                case CUTSCENE -> {
                    String cutsceneId = action.getString("cutscene_id");
                    StoryEngineMod.getInstance().getCutsceneManager().startCutscene(player, cutsceneId);
                }

                case SPAWN_NPC -> {
                    String npcId = action.getString("npc_id");
                    String npcName = action.getString("name");
                    double x = action.getDouble("x");
                    double y = action.getDouble("y");
                    double z = action.getDouble("z");
                    String skin = action.getString("skin");
                    String dialogue = action.getString("dialogue");

                    StoryNPC npc = StoryEngineMod.getInstance().getNpcManager()
                            .spawnNPC(level, npcId, npcName, x, y, z, skin);
                    if (npc != null && dialogue != null && !dialogue.isEmpty()) {
                        npc.setDialogueId(dialogue);
                    }
                }

                case REMOVE_NPC -> {
                    StoryEngineMod.getInstance().getNpcManager()
                            .removeNPC(action.getString("npc_id"));
                }

                case MOVE_NPC -> {
                    String npcId = action.getString("npc_id");
                    double x = action.getDouble("x");
                    double y = action.getDouble("y");
                    double z = action.getDouble("z");
                    float speed = (float) action.getDouble("speed");
                    StoryEngineMod.getInstance().getNpcManager()
                            .moveNPC(npcId, x, y, z, speed > 0 ? speed : 1.0f);
                }

                case NPC_SAY -> {
                    String npcName = action.getString("npc_name");
                    String message = action.getString("message");
                    String color = action.getString("color");
                    if (color.isEmpty()) color = "f";
                    player.sendSystemMessage(
                            Component.literal("§" + color + "[" + npcName + "] §r" + message));
                }

                case TELEPORT_PLAYER -> {
                    double x = action.getDouble("x");
                    double y = action.getDouble("y");
                    double z = action.getDouble("z");
                    float yaw = (float) action.getDouble("yaw");
                    float pitch = (float) action.getDouble("pitch");
                    player.teleportTo(level, x, y, z, yaw, pitch);
                }

                case GIVE_ITEM -> {
                    String itemId = action.getString("item");
                    int count = action.getInt("count");
                    if (count <= 0) count = 1;
                    var item = BuiltInRegistries.ITEM.get(new ResourceLocation(itemId));
                    if (item != null) {
                        player.getInventory().add(new ItemStack(item, count));
                    }
                }

                case SET_BLOCK -> {
                    BlockPos pos = action.getBlockPos("pos");
                    String blockId = action.getString("block");
                    Block block = BuiltInRegistries.BLOCK.get(new ResourceLocation(blockId));
                    if (block != null) {
                        level.setBlock(pos, block.defaultBlockState(), 3);
                    }
                }

                case FILL_BLOCKS -> {
                    BlockPos from = action.getBlockPos("from");
                    BlockPos to = action.getBlockPos("to");
                    String blockId = action.getString("block");
                    Block block = BuiltInRegistries.BLOCK.get(new ResourceLocation(blockId));
                    if (block != null) {
                        BlockState bs = block.defaultBlockState();
                        BlockPos.betweenClosed(from, to).forEach(p -> level.setBlock(p, bs, 3));
                    }
                }

                case PLAY_SOUND -> {
                    String soundId = action.getString("sound");
                    float volume = (float) action.getDouble("volume");
                    float pitch = (float) action.getDouble("pitch");
                    if (volume <= 0) volume = 1.0f;
                    if (pitch <= 0) pitch = 1.0f;
                    SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(new ResourceLocation(soundId));
                    if (sound != null) {
                        player.playNotifySound(sound, SoundSource.MASTER, volume, pitch);
                    }
                }

                case SHOW_TITLE -> {
                    String title = action.getString("text");
                    int fadeIn = action.getInt("fade_in");
                    int stay = action.getInt("stay");
                    int fadeOut = action.getInt("fade_out");
                    if (stay <= 0) stay = 60;
                    if (fadeIn <= 0) fadeIn = 10;
                    if (fadeOut <= 0) fadeOut = 10;
                    player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
                    player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
                }

                case SHOW_SUBTITLE -> {
                    String subtitle = action.getString("text");
                    player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
                }

                case EXECUTE_COMMAND -> {
                    String command = action.getString("command")
                            .replace("{player}", player.getName().getString());
                    server.getCommands().performPrefixedCommand(
                            server.createCommandSourceStack(), command);
                }

                case WAIT -> { /* delay обрабатывается автоматически */ }

                case SET_WEATHER -> {
                    String weather = action.getString("weather");
                    int duration = action.getInt("duration");
                    if (duration <= 0) duration = 6000;
                    switch (weather.toLowerCase()) {
                        case "clear" -> level.setWeatherParameters(duration, 0, false, false);
                        case "rain" -> level.setWeatherParameters(0, duration, true, false);
                        case "thunder" -> level.setWeatherParameters(0, duration, true, true);
                    }
                }

                case SET_TIME -> {
                    long time = action.getLong("time");
                    level.setDayTime(time);
                }

                case EXPLOSION -> {
                    double x = action.getDouble("x");
                    double y = action.getDouble("y");
                    double z = action.getDouble("z");
                    float power = (float) action.getDouble("power");
                    boolean fire = action.getBool("fire");
                    level.explode(null, x, y, z, power,
                            fire ? net.minecraft.world.level.Level.ExplosionInteraction.MOB
                                    : net.minecraft.world.level.Level.ExplosionInteraction.NONE);
                }

                case PARTICLE -> {
                    double x = action.getDouble("x");
                    double y = action.getDouble("y");
                    double z = action.getDouble("z");
                    int count = action.getInt("count");
                    if (count <= 0) count = 10;
                    level.sendParticles(player, ParticleTypes.FLAME, false,
                            x, y, z, count, 0.5, 0.5, 0.5, 0.02);
                }

                case START_QUEST -> {
                    String questId = action.getString("quest_id");
                    StoryEngineMod.getInstance().getQuestManager().startQuest(player, questId);
                }

                case COMPLETE_QUEST -> {
                    String questId = action.getString("quest_id");
                    StoryEngineMod.getInstance().getQuestManager().completeQuest(player, questId);
                }

                case SET_VARIABLE -> {
                    String varName = action.getString("variable");
                    String varValue = action.getString("value");
                    setVariable(player, varName, varValue);
                }

                case CHECK_VARIABLE -> { /* через condition */ }

                case FREEZE_PLAYER -> {
                    frozenPlayers.add(player.getUUID());
                    PlayerStoryState pState = playerStates.get(player.getUUID());
                    if (pState != null) {
                        pState.setFreezePos(new double[]{player.getX(), player.getY(), player.getZ()});
                    }
                    player.setGameMode(GameType.ADVENTURE);
                }

                case UNFREEZE_PLAYER -> {
                    frozenPlayers.remove(player.getUUID());
                    player.setGameMode(GameType.SURVIVAL);
                }

                case FADE_IN, FADE_OUT -> {
                    int duration = action.getInt("duration");
                    if (duration <= 0) duration = 20;
                    StoryEngineMod.getInstance().getCutsceneManager()
                            .sendFadeEffect(player, action.getType() == ActionType.FADE_IN, duration);
                }

                case GIVE_COINS -> {
                    long amount = action.getLong("amount");
                    CurrencyManager.addCoins(player, amount);
                }

                case REMOVE_COINS -> {
                    long amount = action.getLong("amount");
                    CurrencyManager.removeCoins(player, amount);
                }

                case GIVE_XP -> {
                    long amount = action.getLong("amount");
                    LevelingSystem.addXp(player, amount);
                }

                case CHANGE_CHAPTER -> {
                    String chapterId = action.getString("chapter_id");
                    advanceToChapter(player, chapterId);
                }

                case END_STORY -> stopStory(player);

                default -> StoryEngineMod.LOGGER.warn("Unknown action: {}", action.getType());
            }
        } catch (Exception e) {
            StoryEngineMod.LOGGER.error("Error executing action {}: {}", action.getType(), e.getMessage());
        }
    }

    // === Переменные ===

    public void setVariable(ServerPlayer player, String name, Object value) {
        playerVariables.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(name, value);
    }

    public Object getVariable(ServerPlayer player, String name) {
        Map<String, Object> vars = playerVariables.get(player.getUUID());
        return vars != null ? vars.get(name) : null;
    }

    public boolean checkCondition(ServerPlayer player, String condition) {
        try {
            if (condition.contains("==")) {
                String[] parts = condition.split("==", 2);
                Object val = getVariable(player, parts[0].trim());
                return val != null && val.toString().equals(parts[1].trim());
            } else if (condition.contains("!=")) {
                String[] parts = condition.split("!=", 2);
                Object val = getVariable(player, parts[0].trim());
                return val == null || !val.toString().equals(parts[1].trim());
            } else if (condition.contains(">=")) {
                String[] parts = condition.split(">=", 2);
                Object val = getVariable(player, parts[0].trim());
                if (val != null) {
                    try {
                        return Double.parseDouble(val.toString()) >= Double.parseDouble(parts[1].trim());
                    } catch (NumberFormatException ignored) {}
                }
            } else if (condition.contains("<=")) {
                String[] parts = condition.split("<=", 2);
                Object val = getVariable(player, parts[0].trim());
                if (val != null) {
                    try {
                        return Double.parseDouble(val.toString()) <= Double.parseDouble(parts[1].trim());
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception e) {
            StoryEngineMod.LOGGER.warn("Condition error '{}': {}", condition, e.getMessage());
        }
        return true;
    }

    // === Состояние ===

    public PlayerStoryState getPlayerState(UUID uuid) { return playerStates.get(uuid); }
    public boolean isPlayerInStory(UUID uuid) { return playerStates.containsKey(uuid); }
    public boolean isPlayerFrozen(UUID uuid) { return frozenPlayers.contains(uuid); }

    // === Сохранение/загрузка ===

    public void loadStories() {
        StoryEngineMod.LOGGER.info("Loading stories from: {}", storiesFolder.toAbsolutePath());

        try {
            if (Files.exists(storiesFolder)) {
                Files.list(storiesFolder)
                        .filter(p -> p.toString().endsWith(".json"))
                        .forEach(path -> {
                            try {
                                String content = Files.readString(path);
                                JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                                Story story = Story.fromJson(json);
                                registerStory(story);
                            } catch (Exception e) {
                                StoryEngineMod.LOGGER.error("Failed to load story: {}", path.getFileName());
                                e.printStackTrace();
                            }
                        });
            }
        } catch (IOException e) {
            StoryEngineMod.LOGGER.error("Failed to scan stories folder", e);
        }

        StoryEngineMod.LOGGER.info("Total stories loaded: {}", stories.size());
    }

    public void saveStory(Story story) {
        Path path = storiesFolder.resolve(story.getId() + ".json");
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            Files.writeString(path, gson.toJson(story.toJson()));
        } catch (IOException e) {
            StoryEngineMod.LOGGER.error("Failed to save story: {}", story.getId(), e);
        }
    }

    public void saveAll() {
        for (Story story : stories.values()) saveStory(story);
    }

    // === Внутренние классы ===

    public static class PlayerStoryState {
        private final UUID playerUUID;
        private final String storyId;
        private String currentChapter;
        private int currentActionIndex;
        private double[] freezePos;

        public PlayerStoryState(UUID playerUUID, String storyId) {
            this.playerUUID = playerUUID;
            this.storyId = storyId;
        }

        public UUID getPlayerUUID() { return playerUUID; }
        public String getStoryId() { return storyId; }
        public String getCurrentChapter() { return currentChapter; }
        public void setCurrentChapter(String ch) { this.currentChapter = ch; }
        public int getCurrentActionIndex() { return currentActionIndex; }
        public void setCurrentActionIndex(int idx) { this.currentActionIndex = idx; }
        public double[] getFreezePos() { return freezePos; }
        public void setFreezePos(double[] pos) { this.freezePos = pos; }
    }

    public static class ScheduledAction {
        public final StoryAction action;
        public int delay;

        public ScheduledAction(StoryAction action, int delay) {
            this.action = action;
            this.delay = delay;
        }
    }
}