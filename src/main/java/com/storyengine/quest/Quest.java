package com.storyengine.quest;

import java.util.ArrayList;
import java.util.List;

public class Quest {
    private final String id;
    private String name;
    private String description;
    private final List<QuestObjective> objectives;
    private String onCompleteAction;
    private final List<String> rewards;
    private String requiredQuest;
    private boolean autoComplete;

    public Quest(String id, String name) {
        this.id = id;
        this.name = name;
        this.description = "";
        this.objectives = new ArrayList<>();
        this.rewards = new ArrayList<>();
        this.autoComplete = true;
    }

    public Quest setDescription(String desc) { this.description = desc; return this; }

    public Quest addObjective(QuestObjective obj) {
        objectives.add(obj);
        return this;
    }

    public Quest onComplete(String action) {
        this.onCompleteAction = action;
        return this;
    }

    public Quest addReward(String itemId, int count) {
        rewards.add(itemId + ":" + count);
        return this;
    }

    public Quest requireQuest(String questId) {
        this.requiredQuest = questId;
        return this;
    }

    public Quest setAutoComplete(boolean auto) {
        this.autoComplete = auto;
        return this;
    }

    public boolean areAllObjectivesComplete() {
        return objectives.stream().allMatch(QuestObjective::isCompleted);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<QuestObjective> getObjectives() { return objectives; }
    public String getOnCompleteAction() { return onCompleteAction; }
    public List<String> getRewards() { return rewards; }
    public String getRequiredQuest() { return requiredQuest; }
    public boolean isAutoComplete() { return autoComplete; }
}