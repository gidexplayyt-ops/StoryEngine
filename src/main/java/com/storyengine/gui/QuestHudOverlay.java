package com.storyengine.gui;

import com.storyengine.config.StoryEngineConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class QuestHudOverlay implements IGuiOverlay {
    private static String questName = "";
    private static final List<ObjectiveDisplay> objectives = new ArrayList<>();

    public static void setActiveQuest(String name, List<ObjectiveDisplay> objs) {
        questName = name;
        objectives.clear();
        objectives.addAll(objs);
    }

    public static void clear() {
        questName = "";
        objectives.clear();
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick,
                       int screenWidth, int screenHeight) {
        if (!StoryEngineConfig.HUD_QUESTS_ENABLED.get()) return;
        if (questName.isEmpty() && objectives.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        int x = 5;
        int y = 5;

        int bgHeight = 18 + objectives.size() * 12;
        graphics.fill(x - 2, y - 2, x + 160, y + bgHeight, 0x80000000);

        graphics.drawString(mc.font, "§6§l⚔ " + questName, x, y, 0xFFAA00, true);
        y += 14;

        for (ObjectiveDisplay obj : objectives) {
            String icon = obj.completed ? "§a✓" : "§7○";
            String color = obj.completed ? "§a§m" : "§f";
            String progress = " §7[" + obj.current + "/" + obj.required + "]";
            graphics.drawString(mc.font, icon + " " + color + obj.description + progress,
                    x + 4, y, 0xFFFFFF, false);
            y += 12;
        }
    }

    public static class ObjectiveDisplay {
        public final String description;
        public final int current;
        public final int required;
        public final boolean completed;

        public ObjectiveDisplay(String desc, int current, int required, boolean done) {
            this.description = desc;
            this.current = current;
            this.required = required;
            this.completed = done;
        }
    }
}