package com.storyengine.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.*;

public class Story {
    private final String id;
    private String name;
    private String author;
    private String description;
    private String version;
    private final Map<String, StoryChapter> chapters;
    private final List<String> chapterOrder;
    private String startChapterId;

    public Story(String id, String name) {
        this.id = id;
        this.name = name;
        this.author = "Unknown";
        this.description = "";
        this.version = "1.0";
        this.chapters = new LinkedHashMap<>();
        this.chapterOrder = new ArrayList<>();
    }

    public Story setAuthor(String author) { this.author = author; return this; }
    public Story setDescription(String desc) { this.description = desc; return this; }
    public Story setVersion(String version) { this.version = version; return this; }

    public Story addChapter(StoryChapter chapter) {
        chapters.put(chapter.getId(), chapter);
        chapterOrder.add(chapter.getId());
        if (startChapterId == null) startChapterId = chapter.getId();
        return this;
    }

    public Story setStartChapter(String chapterId) {
        this.startChapterId = chapterId;
        return this;
    }

    public StoryChapter getChapter(String id) { return chapters.get(id); }

    public StoryChapter getStartChapter() { return chapters.get(startChapterId); }

    public String getNextChapterId(String currentChapterId) {
        StoryChapter current = chapters.get(currentChapterId);
        if (current != null && current.getNextChapterId() != null) {
            return current.getNextChapterId();
        }
        int idx = chapterOrder.indexOf(currentChapterId);
        if (idx >= 0 && idx < chapterOrder.size() - 1) {
            return chapterOrder.get(idx + 1);
        }
        return null;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getAuthor() { return author; }
    public String getDescription() { return description; }
    public Map<String, StoryChapter> getChapters() { return chapters; }
    public List<String> getChapterOrder() { return chapterOrder; }
    public String getStartChapterId() { return startChapterId; }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.addProperty("author", author);
        json.addProperty("description", description);
        json.addProperty("version", version);
        json.addProperty("startChapter", startChapterId);

        JsonArray chaptersArray = new JsonArray();
        for (String chapterId : chapterOrder) {
            StoryChapter ch = chapters.get(chapterId);
            if (ch != null) chaptersArray.add(ch.toJson());
        }
        json.add("chapters", chaptersArray);
        return json;
    }

    public static Story fromJson(JsonObject json) {
        Story story = new Story(
                json.get("id").getAsString(),
                json.get("name").getAsString()
        );
        if (json.has("author")) story.setAuthor(json.get("author").getAsString());
        if (json.has("description")) story.setDescription(json.get("description").getAsString());
        if (json.has("version")) story.setVersion(json.get("version").getAsString());

        if (json.has("chapters")) {
            json.getAsJsonArray("chapters").forEach(e ->
                    story.addChapter(StoryChapter.fromJson(e.getAsJsonObject())));
        }
        if (json.has("startChapter"))
            story.setStartChapter(json.get("startChapter").getAsString());

        return story;
    }
}