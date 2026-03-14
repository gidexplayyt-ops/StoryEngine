package com.storyengine.gui;

import com.storyengine.network.NPCInteractPacket;
import com.storyengine.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class DialogueScreen extends Screen {
    private final String speakerName;
    private final String dialogueText;
    private final List<String> choices;

    private int revealedChars = 0;
    private int tickCounter = 0;
    private boolean textFullyRevealed = false;

    private static final int BOX_HEIGHT = 120;
    private static final int BOX_MARGIN = 20;
    private static final int PADDING = 15;

    public DialogueScreen(String speakerName, String text, List<String> choices) {
        super(Component.literal("Dialogue"));
        this.speakerName = speakerName;
        this.dialogueText = text;
        this.choices = choices != null ? choices : new ArrayList<>();
    }

    @Override
    protected void init() {
        super.init();
        if (!choices.isEmpty() && textFullyRevealed) addChoiceButtons();
    }

    private void addChoiceButtons() {
        clearWidgets();
        int boxY = this.height - BOX_HEIGHT - BOX_MARGIN;
        int buttonY = boxY + 60;
        int buttonWidth = this.width - BOX_MARGIN * 2 - PADDING * 2;

        for (int i = 0; i < choices.size(); i++) {
            final int idx = i;
            addRenderableWidget(Button.builder(
                            Component.literal("§e▸ " + choices.get(i)),
                            btn -> NetworkHandler.sendToServer(
                                    new NPCInteractPacket(NPCInteractPacket.InteractType.DIALOGUE_CHOICE, idx)))
                    .pos(BOX_MARGIN + PADDING, buttonY + i * 22)
                    .size(buttonWidth, 20).build());
        }
    }

    @Override
    public void tick() {
        super.tick();
        tickCounter++;
        if (!textFullyRevealed) {
            revealedChars += 1;
            if (revealedChars >= dialogueText.length()) {
                revealedChars = dialogueText.length();
                textFullyRevealed = true;
                if (!choices.isEmpty()) addChoiceButtons();
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Полупрозрачный фон
        graphics.fill(0, 0, this.width, this.height, 0x40000000);

        int boxX = BOX_MARGIN;
        int boxY = this.height - BOX_HEIGHT - BOX_MARGIN;
        int boxWidth = this.width - BOX_MARGIN * 2;
        int totalHeight = BOX_HEIGHT;
        if (!choices.isEmpty() && textFullyRevealed) totalHeight += choices.size() * 22 + 10;

        // Рамка
        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + totalHeight, 0xCC1a1a2e);
        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + 2, 0xFFe056fd);
        graphics.fill(boxX, boxY + totalHeight - 1, boxX + boxWidth, boxY + totalHeight, 0xFF6c63ff);
        graphics.fill(boxX, boxY, boxX + 1, boxY + totalHeight, 0xFF6c63ff);
        graphics.fill(boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + totalHeight, 0xFF6c63ff);

        // Имя
        int nameWidth = this.font.width(speakerName) + 20;
        graphics.fill(boxX + 10, boxY - 12, boxX + 10 + nameWidth, boxY + 2, 0xFF2d2d5e);
        graphics.fill(boxX + 10, boxY - 12, boxX + 10 + nameWidth, boxY - 11, 0xFFe056fd);
        graphics.drawString(this.font, "§d§l" + speakerName, boxX + 20, boxY - 9, 0xFFFFFF, true);

        // Текст
        String display = dialogueText.substring(0, Math.min(revealedChars, dialogueText.length()));
        graphics.drawWordWrap(this.font, Component.literal("§f" + display),
                boxX + PADDING, boxY + 30, boxWidth - PADDING * 2, 0xFFFFFF);

        // Подсказка
        if (textFullyRevealed && choices.isEmpty()) {
            if (tickCounter % 40 < 30) {
                String hint = "§7[ЛКМ / Пробел — продолжить]";
                graphics.drawString(this.font, hint,
                        boxX + boxWidth - this.font.width(hint) - PADDING,
                        boxY + totalHeight - 15, 0x999999, false);
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (!textFullyRevealed) {
                revealedChars = dialogueText.length();
                textFullyRevealed = true;
                if (!choices.isEmpty()) addChoiceButtons();
                return true;
            } else if (choices.isEmpty()) {
                NetworkHandler.sendToServer(
                        new NPCInteractPacket(NPCInteractPacket.InteractType.DIALOGUE_ADVANCE, 0));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Space / Enter
        if (keyCode == 32 || keyCode == 257) {
            if (!textFullyRevealed) {
                revealedChars = dialogueText.length();
                textFullyRevealed = true;
                if (!choices.isEmpty()) addChoiceButtons();
            } else if (choices.isEmpty()) {
                NetworkHandler.sendToServer(
                        new NPCInteractPacket(NPCInteractPacket.InteractType.DIALOGUE_ADVANCE, 0));
            }
            return true;
        }

        // ESC
        if (keyCode == 256) {
            NetworkHandler.sendToServer(
                    new NPCInteractPacket(NPCInteractPacket.InteractType.DIALOGUE_ADVANCE, -1));
            this.onClose();
            return true;
        }

        // Цифры 1-9
        if (textFullyRevealed && !choices.isEmpty()) {
            int num = keyCode - 49;
            if (num >= 0 && num < choices.size()) {
                NetworkHandler.sendToServer(
                        new NPCInteractPacket(NPCInteractPacket.InteractType.DIALOGUE_CHOICE, num));
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}