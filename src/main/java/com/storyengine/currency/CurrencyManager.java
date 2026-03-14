package com.storyengine.currency;

import com.storyengine.config.StoryEngineConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CurrencyManager {
    private static final Map<UUID, Long> balances = new ConcurrentHashMap<>();

    public static boolean isEnabled() {
        return StoryEngineConfig.CURRENCY_ENABLED.get();
    }

    public static long getBalance(UUID uuid) {
        return balances.getOrDefault(uuid,
                (long) StoryEngineConfig.CURRENCY_START_AMOUNT.get());
    }

    public static void setBalance(UUID uuid, long amount) {
        balances.put(uuid, Math.max(0, amount));
    }

    public static boolean addCoins(ServerPlayer player, long amount) {
        if (!isEnabled()) return false;
        long current = getBalance(player.getUUID());
        setBalance(player.getUUID(), current + amount);
        player.sendSystemMessage(Component.literal(
                "§6§l+§e" + amount + " монет §7(всего: " + (current + amount) + ")"));
        return true;
    }

    public static boolean removeCoins(ServerPlayer player, long amount) {
        if (!isEnabled()) return false;
        long current = getBalance(player.getUUID());
        if (current < amount) {
            player.sendSystemMessage(Component.literal(
                    "§cНедостаточно монет! Нужно: " + amount + ", у вас: " + current));
            return false;
        }
        setBalance(player.getUUID(), current - amount);
        player.sendSystemMessage(Component.literal(
                "§c§l-§e" + amount + " монет §7(осталось: " + (current - amount) + ")"));
        return true;
    }

    public static boolean hasCoins(UUID uuid, long amount) {
        return getBalance(uuid) >= amount;
    }

    public static void remove(UUID uuid) {
        balances.remove(uuid);
    }
}