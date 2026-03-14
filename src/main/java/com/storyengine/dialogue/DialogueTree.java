package com.storyengine.dialogue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class DialogueTree {
    private final String id;
    private String name;
    private final Map<String, DialogueNode> nodes;
    private String startNodeId;
    private String onCompleteAction;

    public DialogueTree(String id) {
        this.id = id;
        this.name = id;
        this.nodes = new LinkedHashMap<>();
    }

    public DialogueTree setName(String name) {
        this.name = name;
        return this;
    }

    public DialogueTree addNode(DialogueNode node) {
        nodes.put(node.getId(), node);
        if (startNodeId == null) startNodeId = node.getId();
        return this;
    }

    public DialogueTree setStartNode(String nodeId) {
        this.startNodeId = nodeId;
        return this;
    }

    public DialogueTree onComplete(String action) {
        this.onCompleteAction = action;
        return this;
    }

    public DialogueNode getNode(String id) {
        return nodes.get(id);
    }

    public DialogueNode getStartNode() {
        return nodes.get(startNodeId);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getStartNodeId() { return startNodeId; }
    public String getOnCompleteAction() { return onCompleteAction; }
    public Map<String, DialogueNode> getNodes() { return nodes; }

    // === JSON ===

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.addProperty("startNode", startNodeId);
        if (onCompleteAction != null) json.addProperty("onComplete", onCompleteAction);

        JsonArray nodesArray = new JsonArray();
        for (DialogueNode node : nodes.values()) {
            JsonObject nj = new JsonObject();
            nj.addProperty("id", node.getId());
            nj.addProperty("speaker", node.getSpeakerName());
            nj.addProperty("text", node.getText());
            if (node.getNextNodeId() != null) nj.addProperty("next", node.getNextNodeId());
            if (node.getAutoAdvanceDelay() > 0) nj.addProperty("autoAdvance", node.getAutoAdvanceDelay());
            if (node.getAction() != null) nj.addProperty("action", node.getAction());

            if (node.hasChoices()) {
                JsonArray choicesArr = new JsonArray();
                for (DialogueChoice choice : node.getChoices()) {
                    JsonObject cj = new JsonObject();
                    cj.addProperty("text", choice.getText());
                    if (choice.getNextNodeId() != null) cj.addProperty("next", choice.getNextNodeId());
                    if (choice.getAction() != null) cj.addProperty("action", choice.getAction());
                    if (choice.getCondition() != null) cj.addProperty("condition", choice.getCondition());
                    if (choice.getSetVariable() != null) {
                        cj.addProperty("setVar", choice.getSetVariable());
                        cj.addProperty("varValue", choice.getVariableValue());
                    }
                    choicesArr.add(cj);
                }
                nj.add("choices", choicesArr);
            }
            nodesArray.add(nj);
        }
        json.add("nodes", nodesArray);
        return json;
    }

    public static DialogueTree fromJson(JsonObject json) {
        DialogueTree tree = new DialogueTree(json.get("id").getAsString());
        if (json.has("name")) tree.setName(json.get("name").getAsString());
        if (json.has("onComplete")) tree.onComplete(json.get("onComplete").getAsString());

        if (json.has("nodes")) {
            json.getAsJsonArray("nodes").forEach(elem -> {
                JsonObject nj = elem.getAsJsonObject();
                DialogueNode node = new DialogueNode(
                        nj.get("id").getAsString(),
                        nj.has("speaker") ? nj.get("speaker").getAsString() : "",
                        nj.get("text").getAsString()
                );

                if (nj.has("next")) node.setNext(nj.get("next").getAsString());
                if (nj.has("autoAdvance")) node.setAutoAdvance(nj.get("autoAdvance").getAsInt());
                if (nj.has("action")) node.setAction(nj.get("action").getAsString());

                if (nj.has("choices")) {
                    nj.getAsJsonArray("choices").forEach(ce -> {
                        JsonObject cj = ce.getAsJsonObject();
                        node.addChoice(new DialogueChoice(
                                cj.get("text").getAsString(),
                                cj.has("next") ? cj.get("next").getAsString() : null,
                                cj.has("action") ? cj.get("action").getAsString() : null,
                                cj.has("condition") ? cj.get("condition").getAsString() : null,
                                cj.has("setVar") ? cj.get("setVar").getAsString() : null,
                                cj.has("varValue") ? cj.get("varValue").getAsString() : null
                        ));
                    });
                }
                tree.addNode(node);
            });
        }

        if (json.has("startNode")) tree.setStartNode(json.get("startNode").getAsString());
        return tree;
    }
}