package com.storyengine.gui;

import com.storyengine.config.StoryEngineConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

@OnlyIn(Dist.CLIENT)
public class StaminaHudOverlay implements IGuiOverlay {
    private static double stamina = 100;
    private static double maxStamina = 100;
    private static boolean exhausted = false;

    public static void updateData(double s, double max, boolean ex) {
        stamina = s;
        maxStamina = max;
        exhausted = ex;
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick,
                       int screenWidth, int screenHeight) {
        if (!StoryEngineConfig.STAMINA_ENABLED.get()) return;
        if (!StoryEngineConfig.HUD_STAMINA_ENABLED.get()) return;

        Minecraft mc = Minecraft.getInstance();
        int barWidth = 80;
        int barHeight = 5;
        int x = screenWidth / 2 - barWidth / 2;
        int y = screenHeight - 55;

        double percentage = maxStamina > 0 ? stamina / maxStamina : 0;

        // Фон
        graphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0x80000000);

        // Полоска
        int fillWidth = (int) (barWidth * percentage);
        int color;
        if (exhausted) color = 0xFFFF0000;
        else if (percentage < 0.3) color = 0xFFFF6600;
        else if (percentage < 0.6) color = 0xFFFFFF00;
        else color = 0xFF00CC00;

        graphics.fill(x, y, x + fillWidth, y + barHeight, color);

        // Текст
        String text = exhausted ? "§c§lИСТОЩЁН" : "§7" + (int) stamina + "/" + (int) maxStamina;
        int textWidth = mc.font.width(text);
        graphics.drawString(mc.font, text,
                x + barWidth / 2 - textWidth / 2, y - 10, 0xFFFFFF, true);
    }
}