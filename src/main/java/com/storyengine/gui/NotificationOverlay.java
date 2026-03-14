package com.storyengine.gui;

import com.storyengine.config.StoryEngineConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class NotificationOverlay implements IGuiOverlay {
    private static final List<Notification> notifications = new ArrayList<>();

    public static void add(String text, int durationTicks, int color) {
        notifications.add(new Notification(text, durationTicks, color));
        if (notifications.size() > 5) notifications.remove(0);
    }

    public static void addCoins(long amount) {
        String text = (amount >= 0 ? "§6+" : "§c") + amount + " §eмонет";
        add(text, 60, 0xFFAA00);
    }

    public static void addQuest(String text) {
        add("§a⚔ " + text, 80, 0x55FF55);
    }

    public static void addXp(long amount) {
        add("§b+" + amount + " XP", 40, 0x55FFFF);
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick,
                       int screenWidth, int screenHeight) {
        if (!StoryEngineConfig.HUD_NOTIFICATIONS_ENABLED.get()) return;
        if (notifications.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        int y = 40;

        Iterator<Notification> it = notifications.iterator();
        while (it.hasNext()) {
            Notification n = it.next();
            n.tick();

            if (n.isExpired()) {
                it.remove();
                continue;
            }

            float alpha = n.getAlpha();
            int bgAlpha = (int) (alpha * 128);
            int textWidth = mc.font.width(n.text);

            // Анимация слайда
            float slideIn = Math.min(1.0f, n.age / 5.0f);
            int x = (int) (5 * slideIn - (1.0f - slideIn) * textWidth);

            graphics.fill(x - 2, y - 1, x + textWidth + 4, y + 10, bgAlpha << 24);
            graphics.drawString(mc.font, n.text, x, y,
                    ((int) (alpha * 255) << 24) | (n.color & 0xFFFFFF), false);
            y += 14;
        }
    }

    private static class Notification {
        final String text;
        final int duration;
        final int color;
        int age;

        Notification(String text, int duration, int color) {
            this.text = text;
            this.duration = duration;
            this.color = color;
            this.age = 0;
        }

        void tick() { age++; }
        boolean isExpired() { return age >= duration; }

        float getAlpha() {
            if (age < 5) return age / 5.0f;
            if (age > duration - 20) return Math.max(0, (duration - age) / 20.0f);
            return 1.0f;
        }
    }
}