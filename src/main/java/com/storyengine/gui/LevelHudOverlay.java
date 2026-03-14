package com.storyengine.gui;

import com.storyengine.config.StoryEngineConfig;
import com.storyengine.network.LevelSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

@OnlyIn(Dist.CLIENT)
public class LevelHudOverlay implements IGuiOverlay {
    private static int level = 1;
    private static long xp = 0;
    private static long xpToNext = 100;
    private static int points = 0;

    public static void updateData(LevelSyncPacket pkt) {
        level = pkt.level;
        xp = pkt.xp;
        xpToNext = pkt.xpToNext;
        points = pkt.points;
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick,
                       int screenWidth, int screenHeight) {
        if (!StoryEngineConfig.LEVELING_ENABLED.get()) return;
        if (!StoryEngineConfig.HUD_LEVEL_ENABLED.get()) return;

        Minecraft mc = Minecraft.getInstance();

        String levelText = "§6Ур. " + level;
        int textWidth = mc.font.width(levelText);
        int x = screenWidth - textWidth - 5;
        int y = 5;

        // Фон
        graphics.fill(x - 3, y - 2, screenWidth - 2, y + 22, 0x80000000);

        // Уровень
        graphics.drawString(mc.font, levelText, x, y, 0xFFAA00, true);

        // XP полоска
        double pct = xpToNext > 0 ? (double) xp / xpToNext : 0;
        int barW = 60;
        int barX = screenWidth - barW - 5;
        int barY = y + 12;
        graphics.fill(barX, barY, barX + barW, barY + 4, 0xFF333333);
        graphics.fill(barX, barY, barX + (int) (barW * pct), barY + 4, 0xFF55FFFF);

        // Очки навыков
        if (points > 0) {
            String pointsText = "§e+" + points + " очков";
            int pw = mc.font.width(pointsText);
            graphics.drawString(mc.font, pointsText,
                    screenWidth - pw - 5, y + 24, 0xFFFF55, true);
        }
    }
}