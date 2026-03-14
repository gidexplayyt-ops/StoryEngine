package com.storyengine.gui.visual;

import com.storyengine.StoryEngineMod;
import com.storyengine.core.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

public class VisualEditorScreen extends Screen {
    private final List<EditorNode> nodes = new ArrayList<>();
    private EditorNode selectedNode = null;
    private EditorNode connectingFrom = null;
    private int nextNodeId = 0;
    private String storyId = "visual_story";

    private int cameraX = 0, cameraY = 0;
    private boolean panning = false;
    private boolean showPalette = true;

    // Редактирование параметров
    private EditorNode editingNode = null;
    private List<EditBox> paramFields = new ArrayList<>();
    private List<String> paramKeys = new ArrayList<>();

    public VisualEditorScreen() {
        super(Component.literal("Visual Story Editor"));
    }

    @Override
    protected void init() {
        int btnX = 5;
        int btnY = this.height - 25;

        addRenderableWidget(Button.builder(Component.literal("§a+ Нода"),
                        b -> showPalette = !showPalette)
                .pos(btnX, btnY).size(60, 20).build());

        addRenderableWidget(Button.builder(Component.literal("§6Сохранить"),
                        b -> saveAsStory())
                .pos(btnX + 65, btnY).size(70, 20).build());

        addRenderableWidget(Button.builder(Component.literal("§cОчистить"),
                        b -> { nodes.clear(); nextNodeId = 0; closeParamEditor(); })
                .pos(btnX + 140, btnY).size(70, 20).build());

        addRenderableWidget(Button.builder(Component.literal("§fЗакрыть"),
                        b -> onClose())
                .pos(btnX + 215, btnY).size(60, 20).build());

        if (nodes.isEmpty()) {
            nodes.add(new EditorNode("node_0", NodeType.START, 100, 200));
            nextNodeId = 1;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Фон
        graphics.fill(0, 0, this.width, this.height, 0xFF1a1a2e);
        drawGrid(graphics);

        // Соединения
        for (EditorNode node : nodes) {
            for (String targetId : node.outputConnections) {
                EditorNode target = findNode(targetId);
                if (target != null) {
                    drawBezier(graphics,
                            node.getOutputX() - cameraX, node.getOutputY() - cameraY,
                            target.getInputX() - cameraX, target.getInputY() - cameraY,
                            0xFFFFFFFF);
                }
            }
        }

        // Линия при соединении
        if (connectingFrom != null) {
            drawBezier(graphics,
                    connectingFrom.getOutputX() - cameraX,
                    connectingFrom.getOutputY() - cameraY,
                    mouseX, mouseY, 0xFF55FF55);
        }

        // Ноды
        for (EditorNode node : nodes) {
            drawNode(graphics, node);
        }

        // Палитра
        if (showPalette) drawPalette(graphics, mouseX, mouseY);

        // Панель параметров
        if (editingNode != null) drawParamPanel(graphics);

        // Инфо
        if (selectedNode != null && editingNode == null) drawNodeInfo(graphics, selectedNode);

        // Заголовок
        graphics.drawString(this.font, "§6§lVisual Editor §7— " + storyId +
                " §8| Нод: " + nodes.size(), 5, 5, 0xFFFFFF, true);
        graphics.drawString(this.font,
                "§8ЛКМ=выбрать/перетащить  ПКМ=удалить  СКМ=скролл  Shift+ЛКМ=параметры  Del=удалить",
                5, 16, 0x888888, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawGrid(GuiGraphics g) {
        int gridSize = 30;
        for (int x = -cameraX % gridSize; x < this.width; x += gridSize)
            g.fill(x, 0, x + 1, this.height, 0x15FFFFFF);
        for (int y = -cameraY % gridSize; y < this.height; y += gridSize)
            g.fill(0, y, this.width, y + 1, 0x15FFFFFF);
    }

    private void drawNode(GuiGraphics g, EditorNode node) {
        int x = node.x - cameraX;
        int y = node.y - cameraY;
        int w = node.width;
        int h = node.height;

        // Вне экрана
        if (x + w < 0 || x > this.width || y + h < 0 || y > this.height) return;

        int bg = node.selected ? 0xDD2d2d5e : 0xCC1e1e3e;
        g.fill(x, y, x + w, y + h, bg);

        int border = node.type.color;
        g.fill(x, y, x + w, y + 2, border);
        g.fill(x, y + h - 2, x + w, y + h, border);
        g.fill(x, y, x + 2, y + h, border);
        g.fill(x + w - 2, y, x + w, y + h, border);

        g.drawString(this.font, node.type.displayName, x + 5, y + 5, 0xFFFFFF, true);
        g.drawString(this.font, "§8" + node.id, x + 5, y + 16, 0x888888, false);

        // Первый параметр
        if (!node.parameters.isEmpty()) {
            var first = node.parameters.entrySet().iterator().next();
            String val = first.getValue();
            if (val.length() > 16) val = val.substring(0, 16) + "..";
            g.drawString(this.font, "§7" + first.getKey() + "=" + val,
                    x + 5, y + 30, 0xAAAAAA, false);
        }

        // Точки входа/выхода
        if (node.type != NodeType.START)
            g.fill(x - 4, y + h / 2 - 4, x + 4, y + h / 2 + 4, 0xFF55FF55);
        if (node.type != NodeType.END)
            g.fill(x + w - 4, y + h / 2 - 4, x + w + 4, y + h / 2 + 4, 0xFFFF5555);
    }

    private void drawBezier(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int midX = (x1 + x2) / 2;
        // Горизонтальные + вертикальные линии
        int minX, maxX, minY, maxY;
        minX = Math.min(x1, midX); maxX = Math.max(x1, midX);
        g.fill(minX, y1 - 1, maxX, y1 + 1, color);
        minY = Math.min(y1, y2); maxY = Math.max(y1, y2);
        g.fill(midX - 1, minY, midX + 1, maxY, color);
        minX = Math.min(midX, x2); maxX = Math.max(midX, x2);
        g.fill(minX, y2 - 1, maxX, y2 + 1, color);
    }

    private void drawPalette(GuiGraphics g, int mx, int my) {
        int px = this.width - 155;
        int py = 35;
        NodeType[] types = NodeType.values();
        g.fill(px - 5, py - 20, this.width - 5, py + types.length * 20 + 5, 0xDD000000);
        g.drawString(this.font, "§e§lДобавить:", px, py - 15, 0xFFFFFF, true);

        for (int i = 0; i < types.length; i++) {
            int by = py + i * 20;
            boolean hover = mx >= px && mx <= this.width - 5 && my >= by && my <= by + 18;
            g.fill(px, by, this.width - 10, by + 18, hover ? 0x60FFFFFF : 0x30FFFFFF);
            g.drawString(this.font, types[i].displayName, px + 5, by + 5, 0xFFFFFF, false);
        }
    }

    private void drawNodeInfo(GuiGraphics g, EditorNode node) {
        int ix = 5, iy = 30;
        int h = 14 + node.parameters.size() * 11;
        g.fill(ix - 2, iy - 2, ix + 200, iy + h, 0xCC000000);
        g.drawString(this.font, "§b" + node.type.displayName + " §7(" + node.id + ")",
                ix, iy, 0xFFFFFF, false);
        iy += 12;
        for (var entry : node.parameters.entrySet()) {
            g.drawString(this.font, "§7" + entry.getKey() + " §f= " + entry.getValue(),
                    ix + 4, iy, 0xCCCCCC, false);
            iy += 11;
        }
        g.drawString(this.font, "§8Shift+ЛКМ для редактирования", ix + 4, iy + 2, 0x666666, false);
    }

    private void drawParamPanel(GuiGraphics g) {
        int px = this.width / 2 - 120;
        int py = 40;
        int pw = 240;
        int ph = 30 + paramKeys.size() * 25 + 30;
        g.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, 0xFF000000);
        g.fill(px, py, px + pw, py + ph, 0xFF2a2a4e);
        g.drawString(this.font, "§e§lРедактирование: §f" + editingNode.id,
                px + 5, py + 5, 0xFFFFFF, true);
    }

    // === Ввод ===

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // Закрыть редактор параметров при клике вне
        if (editingNode != null && button == 0) {
            int px = this.width / 2 - 120;
            int py = 40;
            if (mx < px || mx > px + 240 || my < py || my > py + 300) {
                closeParamEditor();
            }
        }

        // Палитра
        if (showPalette && button == 0) {
            int px = this.width - 155;
            int py = 35;
            NodeType[] types = NodeType.values();
            for (int i = 0; i < types.length; i++) {
                int by = py + i * 20;
                if (mx >= px && mx <= this.width - 5 && my >= by && my <= by + 18) {
                    String id = "node_" + nextNodeId++;
                    nodes.add(new EditorNode(id, types[i],
                            this.width / 2 + cameraX - 70,
                            this.height / 2 + cameraY - 25));
                    showPalette = false;
                    return true;
                }
            }
        }

        // Ноды
        for (int i = nodes.size() - 1; i >= 0; i--) {
            EditorNode node = nodes.get(i);
            int nx = node.x - cameraX;
            int ny = node.y - cameraY;

            // Точка выхода
            if (button == 0 && node.type != NodeType.END
                    && Math.abs(mx - (nx + node.width)) < 8
                    && Math.abs(my - (ny + node.height / 2)) < 8) {
                connectingFrom = node;
                return true;
            }

            if (node.isMouseOver(mx + cameraX, my + cameraY)) {
                if (button == 0) {
                    // Завершить соединение
                    if (connectingFrom != null && connectingFrom != node) {
                        if (!connectingFrom.outputConnections.contains(node.id)) {
                            connectingFrom.outputConnections.add(node.id);
                        }
                        connectingFrom = null;
                        return true;
                    }

                    // Shift+ЛКМ — редактировать параметры
                    if (hasShiftDown()) {
                        openParamEditor(node);
                        return true;
                    }

                    // Выбрать
                    if (selectedNode != null) selectedNode.selected = false;
                    selectedNode = node;
                    node.selected = true;
                    node.dragging = true;
                    node.dragOffsetX = (int) mx + cameraX - node.x;
                    node.dragOffsetY = (int) my + cameraY - node.y;
                    return true;
                }

                // ПКМ — удалить
                if (button == 1) {
                    nodes.remove(node);
                    for (EditorNode other : nodes)
                        other.outputConnections.remove(node.id);
                    if (selectedNode == node) selectedNode = null;
                    if (editingNode == node) closeParamEditor();
                    return true;
                }
            }
        }

        // Отмена соединения
        if (connectingFrom != null) {
            connectingFrom = null;
            return true;
        }

        // СКМ — скролл
        if (button == 2) {
            panning = true;
            return true;
        }

        // Снять выделение
        if (selectedNode != null && button == 0) {
            selectedNode.selected = false;
            selectedNode = null;
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        for (EditorNode node : nodes) node.dragging = false;
        panning = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        for (EditorNode node : nodes) {
            if (node.dragging) {
                node.x = (int) mx + cameraX - node.dragOffsetX;
                node.y = (int) my + cameraY - node.dragOffsetY;
                return true;
            }
        }
        if (panning) {
            cameraX -= (int) dx;
            cameraY -= (int) dy;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Delete
        if (keyCode == 261 && selectedNode != null && editingNode == null) {
            nodes.remove(selectedNode);
            for (EditorNode other : nodes)
                other.outputConnections.remove(selectedNode.id);
            selectedNode = null;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // === Редактор параметров ===

    private void openParamEditor(EditorNode node) {
        closeParamEditor();
        editingNode = node;
        paramKeys.clear();
        paramFields.clear();

        int px = this.width / 2 - 110;
        int py = 65;
        int idx = 0;
        for (var entry : node.parameters.entrySet()) {
            paramKeys.add(entry.getKey());
            EditBox box = new EditBox(this.font, px, py + idx * 25, 220, 18,
                    Component.literal(entry.getKey()));
            box.setMaxLength(256);
            box.setValue(entry.getValue());
            final String key = entry.getKey();
            box.setResponder(value -> editingNode.parameters.put(key, value));
            paramFields.add(box);
            addRenderableWidget(box);
            idx++;
        }
    }

    private void closeParamEditor() {
        editingNode = null;
        for (EditBox box : paramFields) removeWidget(box);
        paramFields.clear();
        paramKeys.clear();
    }

    // === Сохранение ===

    private void saveAsStory() {
        // Сохраняем параметры из полей
        if (editingNode != null) closeParamEditor();

        Story story = new Story(storyId, storyId);
        story.setAuthor("Visual Editor");

        StoryChapter chapter = new StoryChapter("main", "Main");
        chapter.setAutoAdvance(false);

        EditorNode startNode = nodes.stream()
                .filter(n -> n.type == NodeType.START)
                .findFirst().orElse(null);

        if (startNode != null) {
            Set<String> visited = new HashSet<>();
            buildActions(chapter, startNode, 0, visited);
        }

        story.addChapter(chapter);

        if (StoryEngineMod.getInstance().getStoryManager() != null) {
            StoryEngineMod.getInstance().getStoryManager().registerStory(story);
            StoryEngineMod.getInstance().getStoryManager().saveStory(story);
        }

        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.sendSystemMessage(Component.literal(
                    "§aСохранено! §7/story start " + storyId));
        }
    }

    private void buildActions(StoryChapter chapter, EditorNode node,
                              int delay, Set<String> visited) {
        if (visited.contains(node.id)) return;
        visited.add(node.id);

        StoryAction action = nodeToAction(node, delay);
        if (action != null) chapter.addAction(action);

        for (String nextId : node.outputConnections) {
            EditorNode next = findNode(nextId);
            if (next != null) {
                int nextDelay = 5;
                if (node.type == NodeType.WAIT) {
                    try { nextDelay = Integer.parseInt(node.parameters.getOrDefault("ticks", "20")); }
                    catch (NumberFormatException ignored) {}
                }
                buildActions(chapter, next, nextDelay, visited);
            }
        }
    }

    private StoryAction nodeToAction(EditorNode n, int delay) {
        Map<String, String> p = n.parameters;
        return switch (n.type) {
            case START -> null;
            case DIALOGUE -> new StoryAction(ActionType.DIALOGUE).delay(delay)
                    .param("dialogue_id", p.getOrDefault("dialogue_id", ""));
            case NPC_SPAWN -> new StoryAction(ActionType.SPAWN_NPC).delay(delay)
                    .param("npc_id", p.getOrDefault("npc_id", ""))
                    .param("name", p.getOrDefault("name", ""))
                    .param("x", p.getOrDefault("x", "0"))
                    .param("y", p.getOrDefault("y", "64"))
                    .param("z", p.getOrDefault("z", "0"))
                    .param("dialogue", p.getOrDefault("dialogue", ""));
            case NPC_SAY -> new StoryAction(ActionType.NPC_SAY).delay(delay)
                    .param("npc_name", p.getOrDefault("npc_name", ""))
                    .param("message", p.getOrDefault("message", ""))
                    .param("color", p.getOrDefault("color", "f"));
            case TELEPORT -> new StoryAction(ActionType.TELEPORT_PLAYER).delay(delay)
                    .param("x", p.getOrDefault("x", "0"))
                    .param("y", p.getOrDefault("y", "64"))
                    .param("z", p.getOrDefault("z", "0"))
                    .param("yaw", p.getOrDefault("yaw", "0"))
                    .param("pitch", p.getOrDefault("pitch", "0"));
            case TITLE -> new StoryAction(ActionType.SHOW_TITLE).delay(delay)
                    .param("text", p.getOrDefault("text", ""))
                    .param("fade_in", p.getOrDefault("fade_in", "10"))
                    .param("stay", p.getOrDefault("stay", "40"))
                    .param("fade_out", p.getOrDefault("fade_out", "10"));
            case SOUND -> new StoryAction(ActionType.PLAY_SOUND).delay(delay)
                    .param("sound", p.getOrDefault("sound", ""))
                    .param("volume", p.getOrDefault("volume", "1.0"))
                    .param("pitch", p.getOrDefault("pitch", "1.0"));
            case WAIT -> new StoryAction(ActionType.WAIT).delay(delay);
            case QUEST -> new StoryAction(ActionType.START_QUEST).delay(delay)
                    .param("quest_id", p.getOrDefault("quest_id", ""));
            case GIVE_ITEM -> new StoryAction(ActionType.GIVE_ITEM).delay(delay)
                    .param("item", p.getOrDefault("item", "minecraft:diamond"))
                    .param("count", p.getOrDefault("count", "1"));
            case GIVE_COINS -> new StoryAction(ActionType.GIVE_COINS).delay(delay)
                    .param("amount", p.getOrDefault("amount", "100"));
            case GIVE_XP -> new StoryAction(ActionType.GIVE_XP).delay(delay)
                    .param("amount", p.getOrDefault("amount", "50"));
            case COMMAND -> new StoryAction(ActionType.EXECUTE_COMMAND).delay(delay)
                    .param("command", p.getOrDefault("command", ""));
            case FADE_IN -> new StoryAction(ActionType.FADE_IN).delay(delay)
                    .param("duration", p.getOrDefault("duration", "20"));
            case FADE_OUT -> new StoryAction(ActionType.FADE_OUT).delay(delay)
                    .param("duration", p.getOrDefault("duration", "20"));
            case FREEZE -> new StoryAction(ActionType.FREEZE_PLAYER).delay(delay);
            case UNFREEZE -> new StoryAction(ActionType.UNFREEZE_PLAYER).delay(delay);
            case END -> new StoryAction(ActionType.END_STORY).delay(delay);
            default -> null;
        };
    }

    private EditorNode findNode(String id) {
        return nodes.stream().filter(n -> n.id.equals(id)).findFirst().orElse(null);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}