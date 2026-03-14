package com.storyengine;

import com.storyengine.command.*;
import com.storyengine.config.StoryEngineConfig;
import com.storyengine.core.StoryManager;
import com.storyengine.currency.CurrencyManager;
import com.storyengine.cutscene.CutsceneManager;
import com.storyengine.dialogue.DialogueManager;
import com.storyengine.entity.NPCManager;
import com.storyengine.event.StoryEventHandler;
import com.storyengine.gui.*;
import com.storyengine.leveling.LevelingEvents;
import com.storyengine.leveling.LevelingSystem;
import com.storyengine.network.NetworkHandler;
import com.storyengine.quest.QuestManager;
import com.storyengine.registry.ModRegistry;
import com.storyengine.script.ScriptEngine;
import com.storyengine.stamina.StaminaEvents;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(StoryEngineMod.MOD_ID)
public class StoryEngineMod {
    public static final String MOD_ID = "storyengine";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static StoryEngineMod instance;

    private StoryManager storyManager;
    private DialogueManager dialogueManager;
    private CutsceneManager cutsceneManager;
    private QuestManager questManager;
    private NPCManager npcManager;

    public StoryEngineMod() {
        instance = this;
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Конфиг
        StoryEngineConfig.register();

        // MOD шина
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);
        modBus.addListener(this::registerOverlays);
        ModRegistry.register(modBus);

        // FORGE шина
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new StoryEventHandler());
        MinecraftForge.EVENT_BUS.register(new StaminaEvents());
        MinecraftForge.EVENT_BUS.register(new LevelingEvents());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        NetworkHandler.register();
        LOGGER.info("StoryEngine common setup complete");
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ModRegistry.registerRenderers();
            LOGGER.info("StoryEngine client setup complete");
        });
    }

    private void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("quest_hud", new QuestHudOverlay());
        event.registerAboveAll("stamina_hud", new StaminaHudOverlay());
        event.registerAboveAll("level_hud", new LevelHudOverlay());
        event.registerAboveAll("notifications", new NotificationOverlay());
        event.registerAboveAll("cutscene_overlay", new CutsceneOverlay());
        LOGGER.info("StoryEngine overlays registered");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        storyManager = new StoryManager(event.getServer());
        dialogueManager = new DialogueManager();
        cutsceneManager = new CutsceneManager();
        questManager = new QuestManager();
        npcManager = new NPCManager(event.getServer());

        storyManager.loadStories();
        dialogueManager.loadDialogues();
        questManager.loadQuests();
        cutsceneManager.loadExamples();
        ScriptEngine.loadAllScripts();

        LOGGER.info("StoryEngine server started!");
        LOGGER.info("Stories: {}, Dialogues: {}, Quests: {}",
                storyManager.getAllStories().size(),
                dialogueManager.getAllDialogues().size(),
                questManager.getAllQuests().size());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (storyManager != null) storyManager.saveAll();
        if (questManager != null) questManager.saveAll();
        LOGGER.info("StoryEngine data saved");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        StoryCommand.register(event.getDispatcher());
        NPCCommand.register(event.getDispatcher());
        CutsceneCommand.register(event.getDispatcher());
        DialogueCommand.register(event.getDispatcher());
        StaminaCommand.register(event.getDispatcher());
        LevelCommand.register(event.getDispatcher());
        CurrencyCommand.register(event.getDispatcher());
        LOGGER.info("StoryEngine commands registered");
    }

    // Клиентские события
    @Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            // F7 — визуальный редактор
            if (event.getKey() == GLFW.GLFW_KEY_F7
                    && event.getAction() == GLFW.GLFW_PRESS) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen == null) {
                    mc.setScreen(
                            new com.storyengine.gui.visual.VisualEditorScreen());
                }
            }

            // Обработка ESC во время катсцены
            if (event.getKey() == GLFW.GLFW_KEY_ESCAPE
                    && event.getAction() == GLFW.GLFW_PRESS) {
                CutsceneOverlay.handleKeyPress(GLFW.GLFW_KEY_ESCAPE);
            }
        }
    }

    public static StoryEngineMod getInstance() { return instance; }
    public StoryManager getStoryManager() { return storyManager; }
    public DialogueManager getDialogueManager() { return dialogueManager; }
    public CutsceneManager getCutsceneManager() { return cutsceneManager; }
    public QuestManager getQuestManager() { return questManager; }
    public NPCManager getNpcManager() { return npcManager; }
}