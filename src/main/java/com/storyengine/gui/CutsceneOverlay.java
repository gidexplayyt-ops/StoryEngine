package com.storyengine.gui;

import com.storyengine.network.CutscenePacket;
import com.storyengine.network.NPCInteractPacket;
import com.storyengine.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

@OnlyIn(Dist.CLIENT)
public class CutsceneOverlay implements IGuiOverlay {
    private static boolean active = false;
    private static boolean hideHUD = false;
    private static boolean letterbox = false;
    private static boolean fadeIn = false;
    private static boolean fadeOut = false;
    private static int fadeDuration = 20;
    private static int fadeTimer = 0;
    private static int letterboxProgress = 0;
    private static final int LETTERBOX_HEIGHT = 50;
    private static final int LETTERBOX_SPEED = 2;

    public static void handlePacket(CutscenePacket packet) {
        switch (packet.getAction()) {
            case START -> {
                active = true;
                hideHUD = packet.isHideHUD();
                letterbox = packet.isLetterbox();
                letterboxProgress = 0;
            }
            case STOP -> {
                active = false;
                hideHUD = false;
                fadeIn = false;
                fadeOut = false;
            }
            case FADE_IN -> {
                fadeIn = true;
                fadeOut = false;
                fadeDuration = packet.getDuration();
                fadeTimer = 0;
            }
            case FADE_OUT -> {
                fadeOut = true;
                fadeIn = false;
                fadeDuration = packet.getDuration();
                fadeTimer = 0;
            }
        }
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick,
                       int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();

        // Letterbox
        if (letterbox || letterboxProgress > 0) {
            if (letterbox && letterboxProgress < LETTERBOX_HEIGHT) {
                letterboxProgress = Math.min(letterboxProgress + LETTERBOX_SPEED, LETTERBOX_HEIGHT);
            } else if (!letterbox && letterboxProgress > 0) {
                letterboxProgress = Math.max(letterboxProgress - LETTERBOX_SPEED, 0);
            }
            if (letterboxProgress > 0) {
                graphics.fill(0, 0, screenWidth, letterboxProgress, 0xFF000000);
                graphics.fill(0, screenHeight - letterboxProgress,
                        screenWidth, screenHeight, 0xFF000000);
            }
        }

        // Fade
        if (fadeIn || fadeOut) {
            fadeTimer++;
            float progress = fadeDuration > 0 ? (float) fadeTimer / fadeDuration : 1.0f;
            if (progress >= 1.0f) {
                progress = 1.0f;
                fadeIn = false;
                fadeOut = false;
            }
            int alpha;
            if (fadeIn) {
                alpha = (int) (255 * (1.0f - progress));
            } else {
                alpha = (int) (255 * progress);
            }
            if (alpha > 0) {
                graphics.fill(0, 0, screenWidth, screenHeight, (alpha << 24));
            }
        }

        // Подсказка пропуска
        if (active) {
            String skipText = "§7[ESC — пропустить]";
            graphics.drawString(mc.font, skipText,
                    screenWidth - mc.font.width(skipText) - 10,
                    screenHeight - LETTERBOX_HEIGHT - 15, 0x999999, true);
        }
    }

    public static boolean shouldHideHUD() { return active && hideHUD; }
    public static boolean isActive() { return active; }

    public static boolean handleKeyPress(int keyCode) {
        if (active && keyCode == 256) {
            NetworkHandler.sendToServer(
                    new NPCInteractPacket(NPCInteractPacket.InteractType.SKIP_CUTSCENE, 0));
            return true;
        }
        return false;
    }
}