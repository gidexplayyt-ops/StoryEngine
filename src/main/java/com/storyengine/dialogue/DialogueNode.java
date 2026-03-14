package com.storyengine.dialogue;

import java.util.ArrayList;
import java.util.List;

public class DialogueNode {
    private final String id;
    private final String speakerName;
    private final String text;
    private final List<DialogueChoice> choices;
    private String nextNodeId;
    private int autoAdvanceDelay;
    private String action;

    public DialogueNode(String id, String speakerName, String text) {
        this.id = id;
        this.speakerName = speakerName;
        this.text = text;
        this.choices = new ArrayList<>();
        this.autoAdvanceDelay = 0;
    }

    public DialogueNode addChoice(DialogueChoice choice) {
        choices.add(choice);
        return this;
    }

    public DialogueNode addChoice(String text, String nextNodeId) {
        choices.add(new DialogueChoice(text, nextNodeId));
        return this;
    }

    public DialogueNode setNext(String nextNodeId) {
        this.nextNodeId = nextNodeId;
        return this;
    }

    public DialogueNode setAutoAdvance(int ticks) {
        this.autoAdvanceDelay = ticks;
        return this;
    }

    public DialogueNode setAction(String action) {
        this.action = action;
        return this;
    }

    public String getId() { return id; }
    public String getSpeakerName() { return speakerName; }
    public String getText() { return text; }
    public List<DialogueChoice> getChoices() { return choices; }
    public String getNextNodeId() { return nextNodeId; }
    public int getAutoAdvanceDelay() { return autoAdvanceDelay; }
    public String getAction() { return action; }
    public boolean hasChoices() { return !choices.isEmpty(); }
}