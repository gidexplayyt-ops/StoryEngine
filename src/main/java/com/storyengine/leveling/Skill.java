package com.storyengine.leveling;

public enum Skill {
    STRENGTH("Сила", "Увеличивает урон", "§c"),
    VITALITY("Здоровье", "Увеличивает HP", "§a"),
    AGILITY("Ловкость", "Увеличивает скорость", "§b"),
    ENDURANCE("Выносливость", "Увеличивает стамину", "§e"),
    LUCK("Удача", "Увеличивает шанс дропа", "§d");

    public final String displayName;
    public final String description;
    public final String color;

    Skill(String displayName, String description, String color) {
        this.displayName = displayName;
        this.description = description;
        this.color = color;
    }
}