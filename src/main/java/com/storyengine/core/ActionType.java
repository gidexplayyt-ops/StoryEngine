package com.storyengine.core;

public enum ActionType {
    // Сюжет
    DIALOGUE,
    CUTSCENE,
    CHANGE_CHAPTER,
    END_STORY,
    WAIT,

    // NPC
    SPAWN_NPC,
    REMOVE_NPC,
    MOVE_NPC,
    NPC_SAY,
    PLAY_ANIMATION,

    // Игрок
    TELEPORT_PLAYER,
    GIVE_ITEM,
    FREEZE_PLAYER,
    UNFREEZE_PLAYER,

    // Мир
    SET_BLOCK,
    FILL_BLOCKS,
    SET_WEATHER,
    SET_TIME,
    EXPLOSION,
    PARTICLE,

    // Визуал и звук
    SHOW_TITLE,
    SHOW_SUBTITLE,
    PLAY_SOUND,
    FADE_IN,
    FADE_OUT,
    CAMERA_SHAKE,

    // Квесты и переменные
    START_QUEST,
    COMPLETE_QUEST,
    SET_VARIABLE,
    CHECK_VARIABLE,

    // Валюта и уровни
    GIVE_COINS,
    REMOVE_COINS,
    GIVE_XP,

    // Системные
    EXECUTE_COMMAND
}