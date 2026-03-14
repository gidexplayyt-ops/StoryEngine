package com.storyengine.core;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class StoryAction {
    private final ActionType type;
    private final Map<String, Object> parameters;
    private int delay;
    private String condition;

    public StoryAction(ActionType type) {
        this.type = type;
        this.parameters = new HashMap<>();
        this.delay = 0;
        this.condition = null;
    }

    public StoryAction param(String key, Object value) {
        parameters.put(key, value);
        return this;
    }

    public StoryAction delay(int ticks) {
        this.delay = ticks;
        return this;
    }

    public StoryAction condition(String condition) {
        this.condition = condition;
        return this;
    }

    public ActionType getType() { return type; }
    public int getDelay() { return delay; }
    public String getCondition() { return condition; }
    public Map<String, Object> getParameters() { return parameters; }

    public String getString(String key) {
        Object val = parameters.get(key);
        return val != null ? val.toString() : "";
    }

    public int getInt(String key) {
        Object val = parameters.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val != null) {
            try { return Integer.parseInt(val.toString()); }
            catch (Exception ignored) {}
        }
        return 0;
    }

    public double getDouble(String key) {
        Object val = parameters.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        if (val != null) {
            try { return Double.parseDouble(val.toString()); }
            catch (Exception ignored) {}
        }
        return 0.0;
    }

    public long getLong(String key) {
        Object val = parameters.get(key);
        if (val instanceof Number) return ((Number) val).longValue();
        if (val != null) {
            try { return Long.parseLong(val.toString()); }
            catch (Exception ignored) {}
        }
        return 0L;
    }

    public boolean getBool(String key) {
        Object val = parameters.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        if (val != null) return Boolean.parseBoolean(val.toString());
        return false;
    }

    public BlockPos getBlockPos(String prefix) {
        return new BlockPos(
                getInt(prefix + "_x"),
                getInt(prefix + "_y"),
                getInt(prefix + "_z")
        );
    }

    // === JSON ===

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type.name());
        json.addProperty("delay", delay);
        if (condition != null) json.addProperty("condition", condition);

        JsonObject params = new JsonObject();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            if (entry.getValue() instanceof Number) {
                params.addProperty(entry.getKey(), (Number) entry.getValue());
            } else if (entry.getValue() instanceof Boolean) {
                params.addProperty(entry.getKey(), (Boolean) entry.getValue());
            } else {
                params.addProperty(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        json.add("parameters", params);
        return json;
    }

    public static StoryAction fromJson(JsonObject json) {
        ActionType type;
        try {
            type = ActionType.valueOf(json.get("type").getAsString());
        } catch (IllegalArgumentException e) {
            return null;
        }

        StoryAction action = new StoryAction(type);
        if (json.has("delay")) action.delay(json.get("delay").getAsInt());
        if (json.has("condition")) action.condition(json.get("condition").getAsString());

        if (json.has("parameters")) {
            JsonObject params = json.getAsJsonObject("parameters");
            for (String key : params.keySet()) {
                var element = params.get(key);
                if (element.isJsonPrimitive()) {
                    var prim = element.getAsJsonPrimitive();
                    if (prim.isNumber()) action.param(key, prim.getAsNumber());
                    else if (prim.isBoolean()) action.param(key, prim.getAsBoolean());
                    else action.param(key, prim.getAsString());
                }
            }
        }
        return action;
    }
}