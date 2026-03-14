package com.storyengine.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class StoryChapter {
    private final String id;
    private final String name;
    private String description;
    private final List<StoryAction> actions;
    private String nextChapterId;
    private boolean autoAdvance;

    public StoryChapter(String id, String name) {
        this.id = id;
        this.name = name;
        this.description = "";
        this.actions = new ArrayList<>();
        this.autoAdvance = true;
    }

    public StoryChapter addAction(StoryAction action) {
        if (action != null) actions.add(action);
        return this;
    }

    public StoryChapter setNext(String nextChapterId) {
        this.nextChapterId = nextChapterId;
        return this;
    }

    public StoryChapter setDescription(String desc) {
        this.description = desc;
        return this;
    }

    public StoryChapter setAutoAdvance(boolean auto) {
        this.autoAdvance = auto;
        return this;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<StoryAction> getActions() { return actions; }
    public String getNextChapterId() { return nextChapterId; }
    public boolean isAutoAdvance() { return autoAdvance; }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.addProperty("description", description);
        json.addProperty("autoAdvance", autoAdvance);
        if (nextChapterId != null) json.addProperty("nextChapter", nextChapterId);

        JsonArray actionsArray = new JsonArray();
        for (StoryAction action : actions) {
            actionsArray.add(action.toJson());
        }
        json.add("actions", actionsArray);
        return json;
    }

    public static StoryChapter fromJson(JsonObject json) {
        StoryChapter chapter = new StoryChapter(
                json.get("id").getAsString(),
                json.get("name").getAsString()
        );
        if (json.has("description"))
            chapter.setDescription(json.get("description").getAsString());
        if (json.has("autoAdvance"))
            chapter.setAutoAdvance(json.get("autoAdvance").getAsBoolean());
        if (json.has("nextChapter"))
            chapter.setNext(json.get("nextChapter").getAsString());

        if (json.has("actions")) {
            json.getAsJsonArray("actions").forEach(e -> {
                StoryAction action = StoryAction.fromJson(e.getAsJsonObject());
                if (action != null) chapter.addAction(action);
            });
        }
        return chapter;
    }
}