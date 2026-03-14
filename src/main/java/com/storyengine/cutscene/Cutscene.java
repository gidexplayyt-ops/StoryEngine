package com.storyengine.cutscene;

import com.storyengine.core.StoryAction;

import java.util.ArrayList;
import java.util.List;

public class Cutscene {
    private final String id;
    private String name;
    private CameraPath cameraPath;
    private final List<TimedAction> timedActions;
    private boolean hideHUD;
    private boolean letterbox;
    private boolean freezePlayer;

    public Cutscene(String id) {
        this.id = id;
        this.name = id;
        this.cameraPath = new CameraPath();
        this.timedActions = new ArrayList<>();
        this.hideHUD = true;
        this.letterbox = true;
        this.freezePlayer = true;
    }

    public Cutscene setName(String name) { this.name = name; return this; }
    public Cutscene setCameraPath(CameraPath path) { this.cameraPath = path; return this; }
    public Cutscene setHideHUD(boolean hide) { this.hideHUD = hide; return this; }
    public Cutscene setLetterbox(boolean lb) { this.letterbox = lb; return this; }
    public Cutscene setFreezePlayer(boolean freeze) { this.freezePlayer = freeze; return this; }

    public Cutscene addTimedAction(int tick, StoryAction action) {
        timedActions.add(new TimedAction(tick, action));
        return this;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public CameraPath getCameraPath() { return cameraPath; }
    public List<TimedAction> getTimedActions() { return timedActions; }
    public boolean isHideHUD() { return hideHUD; }
    public boolean isLetterbox() { return letterbox; }
    public boolean isFreezePlayer() { return freezePlayer; }
    public int getDuration() { return cameraPath.getTotalDuration(); }

    public static class TimedAction {
        public final int tick;
        public final StoryAction action;

        public TimedAction(int tick, StoryAction action) {
            this.tick = tick;
            this.action = action;
        }
    }
}