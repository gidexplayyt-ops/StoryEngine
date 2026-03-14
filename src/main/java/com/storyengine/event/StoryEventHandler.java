package com.storyengine.event;

import com.storyengine.StoryEngineMod;
import com.storyengine.quest.QuestManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class StoryEventHandler {

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        var mod = StoryEngineMod.getInstance();
        if (mod.getStoryManager() != null) mod.getStoryManager().tick();
        if (mod.getCutsceneManager() != null) mod.getCutsceneManager().tick();
    }

    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            LivingEntity killed = event.getEntity();
            String entityType = killed.getType().builtInRegistryHolder()
                    .key().location().toString();

            var qm = StoryEngineMod.getInstance().getQuestManager();
            if (qm != null) qm.onEntityKilled(player, entityType);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var mod = StoryEngineMod.getInstance();
            if (mod.getStoryManager() != null) mod.getStoryManager().stopStory(player);
            if (mod.getCutsceneManager() != null) mod.getCutsceneManager().stopCutscene(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            var mod = StoryEngineMod.getInstance();
            if (mod.getNpcManager() != null) mod.getNpcManager().reindexFromWorld();
        }
    }
}