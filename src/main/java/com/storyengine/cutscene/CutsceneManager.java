package com.storyengine.cutscene;

import com.storyengine.StoryEngineMod;
import com.storyengine.network.CutscenePacket;
import com.storyengine.network.NetworkHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CutsceneManager {
    private final Map<String, Cutscene> cutscenes = new LinkedHashMap<>();
    private final Map<UUID, ActiveCutscene> activeCutscenes = new ConcurrentHashMap<>();

    public void registerCutscene(Cutscene cutscene) {
        cutscenes.put(cutscene.getId(), cutscene);
        StoryEngineMod.LOGGER.info("Registered cutscene: {}", cutscene.getId());
    }

    public Cutscene getCutscene(String id) {
        return cutscenes.get(id);
    }

    public Collection<Cutscene> getAllCutscenes() {
        return cutscenes.values();
    }

    public void startCutscene(ServerPlayer player, String cutsceneId) {
        Cutscene cutscene = cutscenes.get(cutsceneId);
        if (cutscene == null) {
            player.sendSystemMessage(Component.literal("§cКатсцена '" + cutsceneId + "' не найдена!"));
            return;
        }

        ActiveCutscene active = new ActiveCutscene(player, cutscene);
        activeCutscenes.put(player.getUUID(), active);

        if (cutscene.isFreezePlayer()) {
            active.previousGameMode = player.gameMode.getGameModeForPlayer();
            player.setGameMode(GameType.SPECTATOR);
        }

        NetworkHandler.sendToClient(player, new CutscenePacket(
                CutscenePacket.Action.START, cutscene.isHideHUD(), cutscene.isLetterbox(), 0));

        player.sendSystemMessage(Component.literal("§d§l🎬 " + cutscene.getName()));
    }

    public void stopCutscene(ServerPlayer player) {
        ActiveCutscene active = activeCutscenes.remove(player.getUUID());
        if (active != null) {
            if (active.previousGameMode != null) {
                player.setGameMode(active.previousGameMode);
            }
            player.teleportTo(active.startX, active.startY, active.startZ);
            NetworkHandler.sendToClient(player,
                    new CutscenePacket(CutscenePacket.Action.STOP, false, false, 0));
        }
    }

    public void tick() {
        Iterator<Map.Entry<UUID, ActiveCutscene>> it = activeCutscenes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ActiveCutscene> entry = it.next();
            ActiveCutscene active = entry.getValue();

            ServerPlayer player = active.player.server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                it.remove();
                continue;
            }

            // Камера
            CameraPath.CameraState state = active.cutscene.getCameraPath()
                    .interpolate(active.currentTick);
            if (state != null) {
                player.teleportTo(player.serverLevel(),
                        state.position.x, state.position.y, state.position.z,
                        state.yaw, state.pitch);
            }

            // Timed actions
            for (Cutscene.TimedAction ta : active.cutscene.getTimedActions()) {
                if (ta.tick == active.currentTick) {
                    StoryEngineMod.getInstance().getStoryManager()
                            .executeAction(player, ta.action);
                }
            }

            active.currentTick++;

            if (active.currentTick >= active.cutscene.getDuration()) {
                stopCutscene(player);
            }
        }
    }

    public boolean isInCutscene(UUID uuid) {
        return activeCutscenes.containsKey(uuid);
    }

    public void sendFadeEffect(ServerPlayer player, boolean fadeIn, int duration) {
        NetworkHandler.sendToClient(player, new CutscenePacket(
                fadeIn ? CutscenePacket.Action.FADE_IN : CutscenePacket.Action.FADE_OUT,
                false, false, duration));
    }

    public void loadExamples() {
        CameraPath path = new CameraPath()
                .addPoint(0, 100, 0, 0, 30, 60)
                .addPoint(50, 80, 50, 90, 15, 80)
                .addPoint(100, 70, 0, 180, 0, 60)
                .addPoint(0, 100, 0, 270, -10, 40);

        registerCutscene(new Cutscene("example_cutscene")
                .setName("Обзор мира")
                .setCameraPath(path));
    }

    private static class ActiveCutscene {
        final ServerPlayer player;
        final Cutscene cutscene;
        int currentTick;
        GameType previousGameMode;
        final double startX, startY, startZ;

        ActiveCutscene(ServerPlayer player, Cutscene cutscene) {
            this.player = player;
            this.cutscene = cutscene;
            this.currentTick = 0;
            this.startX = player.getX();
            this.startY = player.getY();
            this.startZ = player.getZ();
        }
    }
}