package com.storyengine.stamina;

import com.storyengine.network.NetworkHandler;
import com.storyengine.network.StaminaSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class StaminaEvents {
    private int syncTimer = 0;

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!StaminaSystem.isEnabled()) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        StaminaData data = StaminaSystem.getData(player.getUUID());

        if (player.isSprinting()) {
            if (!StaminaSystem.consumeSprint(player.getUUID())) {
                player.setSprinting(false);
            }
        } else {
            StaminaSystem.tickRegeneration(player.getUUID());
        }

        syncTimer++;
        if (syncTimer >= 5) {
            syncTimer = 0;
            NetworkHandler.sendToClient(player, new StaminaSyncPacket(
                    data.getStamina(), data.getMaxStamina(), data.isExhausted()));
        }
    }

    @SubscribeEvent
    public void onJump(LivingEvent.LivingJumpEvent event) {
        if (!StaminaSystem.isEnabled()) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            StaminaData data = StaminaSystem.getData(player.getUUID());
            if (data.isExhausted()) {
                player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0.3, 1));
            } else {
                StaminaSystem.consumeJump(player.getUUID());
            }
        }
    }

    @SubscribeEvent
    public void onAttack(LivingAttackEvent event) {
        if (!StaminaSystem.isEnabled()) return;
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            if (!StaminaSystem.getData(player.getUUID()).isExhausted()) {
                StaminaSystem.consumeAttack(player.getUUID());
            }
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        StaminaSystem.remove(event.getEntity().getUUID());
    }
}