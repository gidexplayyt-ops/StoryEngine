package com.storyengine.dialogue;

public class DialogueChoice {
    private final String text;
    private final String nextNodeId;
    private final String action;
    private final String condition;
    private final String setVariable;
    private final String variableValue;

    public DialogueChoice(String text, String nextNodeId) {
        this(text, nextNodeId, null, null, null, null);
    }

    public DialogueChoice(String text, String nextNodeId, String action,
                          String condition, String setVariable, String variableValue) {
        this.text = text;
        this.nextNodeId = nextNodeId;
        this.action = action;
        this.condition = condition;
        this.setVariable = setVariable;
        this.variableValue = variableValue;
    }

    public String getText() { return text; }
    public String getNextNodeId() { return nextNodeId; }
    public String getAction() { return action; }
    public String getCondition() { return condition; }
    public String getSetVariable() { return setVariable; }
    public String getVariableValue() { return variableValue; }

    public static class Builder {
        private String text;
        private String nextNodeId;
        private String action;
        private String condition;
        private String setVariable;
        private String variableValue;

        public Builder(String text) {
            this.text = text;
        }

        public Builder next(String nodeId) {
            this.nextNodeId = nodeId;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder condition(String condition) {
            this.condition = condition;
            return this;
        }

        public Builder setVar(String name, String value) {
            this.setVariable = name;
            this.variableValue = value;
            return this;
        }

        public DialogueChoice build() {
            return new DialogueChoice(text, nextNodeId, action,
                    condition, setVariable, variableValue);
        }
    }
}