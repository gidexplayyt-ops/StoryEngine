package com.storyengine.quest;

public class QuestObjective {
    public enum ObjectiveType {
        KILL_ENTITY,
        COLLECT_ITEM,
        REACH_LOCATION,
        TALK_TO_NPC,
        BREAK_BLOCK,
        PLACE_BLOCK,
        CRAFT_ITEM,
        CUSTOM
    }

    private final String id;
    private final ObjectiveType type;
    private final String target;
    private final int requiredCount;
    private final String description;
    private int currentCount;
    private boolean completed;

    public QuestObjective(String id, ObjectiveType type, String target,
                          int requiredCount, String description) {
        this.id = id;
        this.type = type;
        this.target = target;
        this.requiredCount = requiredCount;
        this.description = description;
        this.currentCount = 0;
        this.completed = false;
    }

    public void increment() { increment(1); }

    public void increment(int amount) {
        currentCount = Math.min(currentCount + amount, requiredCount);
        if (currentCount >= requiredCount) completed = true;
    }

    public void complete() {
        currentCount = requiredCount;
        completed = true;
    }

    public String getId() { return id; }
    public ObjectiveType getType() { return type; }
    public String getTarget() { return target; }
    public int getRequiredCount() { return requiredCount; }
    public int getCurrentCount() { return currentCount; }
    public String getDescription() { return description; }
    public boolean isCompleted() { return completed; }
    public float getProgress() { return (float) currentCount / requiredCount; }
}