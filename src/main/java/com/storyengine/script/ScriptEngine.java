package com.storyengine.script;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.storyengine.StoryEngineMod;
import com.storyengine.core.*;
import com.storyengine.cutscene.*;
import com.storyengine.dialogue.DialogueTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ScriptEngine {

    public static void loadAllScripts() {
        Path folder;
        if (StoryEngineMod.getInstance().getStoryManager() != null) {
            folder = StoryEngineMod.getInstance().getStoryManager().getScriptsFolder();
        } else {
            folder = Path.of("storyengine", "scripts");
        }

        StoryEngineMod.LOGGER.info("Loading scripts from: {}", folder.toAbsolutePath());

        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            StoryEngineMod.LOGGER.error("Failed to create scripts folder", e);
            return;
        }

        try {
            if (Files.exists(folder)) {
                Files.list(folder).forEach(path -> {
                    try {
                        String name = path.getFileName().toString();
                        if (name.endsWith(".json")) loadJsonScript(path);
                        else if (name.endsWith(".script")) loadTextScript(path);
                    } catch (Exception e) {
                        StoryEngineMod.LOGGER.error("Failed to load script: {}",
                                path.getFileName());
                        e.printStackTrace();
                    }
                });
            }
        } catch (IOException e) {
            StoryEngineMod.LOGGER.error("Failed to scan scripts folder", e);
        }
    }

    public static void loadJsonScript(Path file) {
        try {
            String content = Files.readString(file);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            String type = root.has("type") ? root.get("type").getAsString() : "story";

            switch (type) {
                case "story" -> {
                    Story story = Story.fromJson(root);
                    StoryEngineMod.getInstance().getStoryManager().registerStory(story);
                }
                case "dialogue" -> {
                    DialogueTree tree = DialogueTree.fromJson(root);
                    StoryEngineMod.getInstance().getDialogueManager().registerDialogue(tree);
                }
                case "cutscene" -> loadCutsceneScript(root);
            }
        } catch (Exception e) {
            StoryEngineMod.LOGGER.error("Failed to parse JSON script: {}", file.getFileName());
            e.printStackTrace();
        }
    }

    private static void loadCutsceneScript(JsonObject root) {
        String id = root.get("id").getAsString();
        String name = root.has("name") ? root.get("name").getAsString() : id;

        Cutscene cutscene = new Cutscene(id).setName(name);
        if (root.has("hideHUD")) cutscene.setHideHUD(root.get("hideHUD").getAsBoolean());
        if (root.has("letterbox")) cutscene.setLetterbox(root.get("letterbox").getAsBoolean());

        if (root.has("cameraPath")) {
            CameraPath path = new CameraPath();
            root.getAsJsonArray("cameraPath").forEach(e -> {
                JsonObject p = e.getAsJsonObject();
                path.addPoint(p.get("x").getAsDouble(), p.get("y").getAsDouble(),
                        p.get("z").getAsDouble(), p.get("yaw").getAsFloat(),
                        p.get("pitch").getAsFloat(), p.get("duration").getAsInt());
            });
            cutscene.setCameraPath(path);
        }

        StoryEngineMod.getInstance().getCutsceneManager().registerCutscene(cutscene);
    }

    public static void loadTextScript(Path file) {
        try {
            String fileName = file.getFileName().toString();
            String scriptId = fileName.replace(".script", "");

            List<String> lines = Files.readAllLines(file);
            List<StoryAction> actions = new ArrayList<>();

            for (String line : lines) {
                StoryAction action = ScriptParser.parseLine(line);
                if (action != null) actions.add(action);
            }

            if (actions.isEmpty()) return;

            Story story = new Story(scriptId, scriptId);
            story.setAuthor("Script");

            StoryChapter chapter = new StoryChapter(scriptId + "_main", scriptId);
            chapter.setAutoAdvance(false);
            for (StoryAction action : actions) chapter.addAction(action);

            story.addChapter(chapter);
            StoryEngineMod.getInstance().getStoryManager().registerStory(story);

            StoryEngineMod.LOGGER.info("Loaded script '{}' ({} actions)", scriptId, actions.size());
        } catch (Exception e) {
            StoryEngineMod.LOGGER.error("Failed to load text script: {}", file.getFileName());
            e.printStackTrace();
        }
    }
}