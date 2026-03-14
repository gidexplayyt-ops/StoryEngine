package com.storyengine.gui.visual;

public enum NodeType {
    START("§aСтарт", 0xFF00AA00),
    DIALOGUE("§bДиалог", 0xFF0055FF),
    NPC_SPAWN("§eNPC Спавн", 0xFFFFAA00),
    NPC_SAY("§eNPC Говорит", 0xFFFFCC00),
    TELEPORT("§dТелепорт", 0xFFAA00FF),
    TITLE("§fЗаголовок", 0xFFFFFFFF),
    SOUND("§3Звук", 0xFF00AAAA),
    WAIT("§7Ждать", 0xFF888888),
    QUEST("§6Квест", 0xFFFF8800),
    GIVE_ITEM("§aПредмет", 0xFF55FF55),
    GIVE_COINS("§6Монеты", 0xFFFFAA00),
    GIVE_XP("§bОпыт", 0xFF55FFFF),
    COMMAND("§cКоманда", 0xFFFF5555),
    FADE_IN("§7Появление", 0xFF999999),
    FADE_OUT("§8Затемнение", 0xFF666666),
    FREEZE("§cЗаморозить", 0xFFFF0000),
    UNFREEZE("§aРазморозить", 0xFF00FF00),
    CONDITION("§eУсловие", 0xFFFFFF00),
    END("§4Конец", 0xFFAA0000);

    public final String displayName;
    public final int color;

    NodeType(String name, int color) {
        this.displayName = name;
        this.color = color;
    }
}