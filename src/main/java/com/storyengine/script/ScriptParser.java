package com.storyengine.script;

import com.storyengine.core.ActionType;
import com.storyengine.core.StoryAction;

public class ScriptParser {

    public static StoryAction parseLine(String line) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) return null;

        String[] parts = line.split("\\s+", 3);

        int delay;
        String actionName;
        String paramString;

        try {
            delay = Integer.parseInt(parts[0]);
            actionName = parts.length > 1 ? parts[1] : "";
            paramString = parts.length > 2 ? parts[2] : "";
        } catch (NumberFormatException e) {
            delay = 0;
            actionName = parts[0];
            paramString = parts.length > 1 ? parts[1] : "";
            if (parts.length > 2) paramString += " " + parts[2];
        }

        ActionType type;
        try {
            type = ActionType.valueOf(actionName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }

        StoryAction action = new StoryAction(type).delay(delay);
        if (!paramString.isEmpty()) parseParameters(action, paramString);
        return action;
    }

    private static void parseParameters(StoryAction action, String paramString) {
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        String currentKey = null;

        for (int i = 0; i < paramString.length(); i++) {
            char c = paramString.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == '=' && !inQuotes && currentKey == null) {
                currentKey = current.toString().trim();
                current = new StringBuilder();
            } else if (c == ' ' && !inQuotes && currentKey != null) {
                String value = current.toString().trim();
                if (!value.isEmpty()) action.param(currentKey, parseValue(value));
                currentKey = null;
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        if (currentKey != null && current.length() > 0) {
            action.param(currentKey, parseValue(current.toString().trim()));
        }
    }

    private static Object parseValue(String value) {
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        try { return Integer.parseInt(value); } catch (NumberFormatException ignored) {}
        try { return Double.parseDouble(value); } catch (NumberFormatException ignored) {}
        return value;
    }
}