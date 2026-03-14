package com.storyengine.entity;

import com.storyengine.StoryEngineMod;
import com.storyengine.registry.ModRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NPCManager {
    private final MinecraftServer server;
    private final Map<String, StoryNPC> npcRegistry = new ConcurrentHashMap<>();

    public NPCManager(MinecraftServer server) {
        this.server = server;
    }

    public StoryNPC spawnNPC(ServerLevel level, String npcId, String name,
                             double x, double y, double z, String skin) {
        // Удаляем старого с таким ID
        removeNPC(npcId);

        StoryNPC npc = ModRegistry.STORY_NPC.get().create(level);
        if (npc == null) {
            StoryEngineMod.LOGGER.error("Failed to create NPC entity!");
            return null;
        }

        npc.setNPCId(npcId);
        npc.setNPCDisplayName(name);
        npc.setSkinName(skin != null ? skin : "");
        npc.setPos(x, y, z);

        level.addFreshEntity(npc);
        npcRegistry.put(npcId, npc);

        StoryEngineMod.LOGGER.info("Spawned NPC '{}' ({}) at {:.1f}, {:.1f}, {:.1f}",
                name, npcId, x, y, z);
        return npc;
    }

    public void removeNPC(String npcId) {
        StoryNPC npc = npcRegistry.remove(npcId);
        if (npc != null && npc.isAlive()) {
            npc.discard();
            StoryEngineMod.LOGGER.info("Removed NPC: {}", npcId);
        }
    }

    public StoryNPC getNPC(String npcId) {
        StoryNPC npc = npcRegistry.get(npcId);
        if (npc != null && !npc.isAlive()) {
            npcRegistry.remove(npcId);
            return null;
        }
        return npc;
    }

    public void moveNPC(String npcId, double x, double y, double z, float speed) {
        StoryNPC npc = getNPC(npcId);
        if (npc != null) {
            npc.moveToPosition(x, y, z, speed);
        } else {
            StoryEngineMod.LOGGER.warn("Cannot move NPC '{}' — not found", npcId);
        }
    }

    public void setNPCDialogue(String npcId, String dialogueId) {
        StoryNPC npc = getNPC(npcId);
        if (npc != null) {
            npc.setDialogueId(dialogueId);
            StoryEngineMod.LOGGER.info("NPC '{}' dialogue set to '{}'", npcId, dialogueId);
        } else {
            StoryEngineMod.LOGGER.warn("Cannot set dialogue for NPC '{}' — not found", npcId);
        }
    }

    public void setNPCLookAtPlayer(String npcId, boolean look) {
        StoryNPC npc = getNPC(npcId);
        if (npc != null) {
            npc.setLookAtPlayer(look);
        }
    }

    public Map<String, StoryNPC> getAllNPCs() {
        return npcRegistry;
    }

    public void removeAllNPCs() {
        for (StoryNPC npc : npcRegistry.values()) {
            if (npc.isAlive()) npc.discard();
        }
        npcRegistry.clear();
        StoryEngineMod.LOGGER.info("All NPCs removed");
    }

    public void reindexFromWorld() {
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            level.getEntities(ModRegistry.STORY_NPC.get(), new AABB(
                    -30000000, -64, -30000000,
                    30000000, 320, 30000000
            ), e -> true).forEach(npc -> {
                if (npc.getNPCId() != null && !npc.getNPCId().isEmpty()) {
                    npcRegistry.put(npc.getNPCId(), npc);
                }
            });
            count += npcRegistry.size();
        }
        StoryEngineMod.LOGGER.info("Reindexed {} NPCs from world", count);
    }
}