package com.storyengine.gui.visual;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EditorNode {
    public String id;
    public NodeType type;
    public int x, y;
    public int width = 140;
    public int height = 50;
    public Map<String, String> parameters = new LinkedHashMap<>();
    public List<String> outputConnections = new ArrayList<>();
    public boolean selected = false;
    public boolean dragging = false;
    public int dragOffsetX, dragOffsetY;

    public EditorNode(String id, NodeType type, int x, int y) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
        initDefaults();
    }

    private void initDefaults() {
        switch (type) {
            case DIALOGUE -> parameters.put("dialogue_id", "");
            case NPC_SPAWN -> {
                parameters.put("npc_id", "");
                parameters.put("name", "");
                parameters.put("x", "0");
                parameters.put("y", "64");
                parameters.put("z", "0");
                parameters.put("dialogue", "");
            }
            case NPC_SAY -> {
                parameters.put("npc_name", "");
                parameters.put("message", "");
                parameters.put("color", "f");
            }
            case TELEPORT -> {
                parameters.put("x", "0");
                parameters.put("y", "64");
                parameters.put("z", "0");
                parameters.put("yaw", "0");
                parameters.put("pitch", "0");
            }
            case TITLE -> {
                parameters.put("text", "");
                parameters.put("fade_in", "10");
                parameters.put("stay", "40");
                parameters.put("fade_out", "10");
            }
            case SOUND -> {
                parameters.put("sound", "minecraft:entity.player.levelup");
                parameters.put("volume", "1.0");
                parameters.put("pitch", "1.0");
            }
            case WAIT -> parameters.put("ticks", "20");
            case QUEST -> parameters.put("quest_id", "");
            case GIVE_ITEM -> {
                parameters.put("item", "minecraft:diamond");
                parameters.put("count", "1");
            }
            case GIVE_COINS -> parameters.put("amount", "100");
            case GIVE_XP -> parameters.put("amount", "50");
            case COMMAND -> parameters.put("command", "");
            case FADE_IN, FADE_OUT -> parameters.put("duration", "20");
            case CONDITION -> {
                parameters.put("variable", "");
                parameters.put("value", "");
            }
            default -> {}
        }
    }

    public boolean isMouseOver(double mx, double my) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }

    public int getOutputX() { return x + width; }
    public int getOutputY() { return y + height / 2; }
    public int getInputX() { return x; }
    public int getInputY() { return y + height / 2; }
}