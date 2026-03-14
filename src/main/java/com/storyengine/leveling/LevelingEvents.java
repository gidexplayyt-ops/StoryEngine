package com.storyengine.leveling;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class LevelingEvents {

    @SubscribeEvent
    public void onKill(LivingDeathEvent event) {
        if (!LevelingSystem.isEnabled()) return;
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            String type = event.getEntity().getType().toString();
            long xp = switch (type) {
                case "entity.minecraft.zombie", "entity.minecraft.skeleton" -> 10;
                case "entity.minecraft.creeper" -> 15;
                case "entity.minecraft.spider" -> 8;
                case "entity.minecraft.enderman" -> 25;
                case "entity.minecraft.wither_skeleton" -> 30;
                case "entity.minecraft.wither" -> 500;
                case "entity.minecraft.ender_dragon" -> 1000;
                default -> 5;
            };
            LevelingSystem.addXp(player, xp);
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (LevelingSystem.isEnabled()) {
                LevelingSystem.applyStatBonuses(player);
                LevelingSystem.syncToClient(player);
            }
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LevelingSystem.remove(event.getEntity().getUUID());
    }
}