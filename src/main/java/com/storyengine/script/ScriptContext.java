package com.storyengine.script;

import com.storyengine.StoryEngineMod;
import com.storyengine.core.*;

public class ScriptContext {
    private final Story story;
    private StoryChapter currentChapter;

    private ScriptContext(String id, String name) {
        this.story = new Story(id, name);
    }

    public static ScriptContext createStory(String id, String name) {
        return new ScriptContext(id, name);
    }

    public ScriptContext author(String a) { story.setAuthor(a); return this; }
    public ScriptContext description(String d) { story.setDescription(d); return this; }

    public ScriptContext chapter(String id, String name) {
        if (currentChapter != null) story.addChapter(currentChapter);
        currentChapter = new StoryChapter(id, name);
        return this;
    }

    public ScriptContext endChapter() {
        if (currentChapter != null) { story.addChapter(currentChapter); currentChapter = null; }
        return this;
    }

    private ScriptContext add(StoryAction a) {
        if (currentChapter != null) currentChapter.addAction(a);
        return this;
    }

    public ScriptContext wait(int t) { return add(new StoryAction(ActionType.WAIT).delay(t)); }
    public ScriptContext fadeOut(int d) { return add(new StoryAction(ActionType.FADE_OUT).param("duration",d)); }
    public ScriptContext fadeIn(int d) { return add(new StoryAction(ActionType.FADE_IN).param("duration",d)); }

    public ScriptContext title(String text, int fi, int s, int fo) {
        return add(new StoryAction(ActionType.SHOW_TITLE)
                .param("text",text).param("fade_in",fi).param("stay",s).param("fade_out",fo));
    }
    public ScriptContext subtitle(String t) { return add(new StoryAction(ActionType.SHOW_SUBTITLE).param("text",t)); }

    public ScriptContext spawnNPC(String id, String name, double x, double y, double z) {
        return add(new StoryAction(ActionType.SPAWN_NPC)
                .param("npc_id",id).param("name",name).param("x",x).param("y",y).param("z",z));
    }
    public ScriptContext removeNPC(String id) { return add(new StoryAction(ActionType.REMOVE_NPC).param("npc_id",id)); }
    public ScriptContext moveNPC(String id, double x, double y, double z) {
        return add(new StoryAction(ActionType.MOVE_NPC).param("npc_id",id).param("x",x).param("y",y).param("z",z));
    }
    public ScriptContext npcSay(String name, String msg) {
        return add(new StoryAction(ActionType.NPC_SAY).param("npc_name",name).param("message",msg));
    }

    public ScriptContext dialogue(String id) { return add(new StoryAction(ActionType.DIALOGUE).param("dialogue_id",id)); }
    public ScriptContext cutscene(String id) { return add(new StoryAction(ActionType.CUTSCENE).param("cutscene_id",id)); }

    public ScriptContext teleport(double x, double y, double z) {
        return add(new StoryAction(ActionType.TELEPORT_PLAYER).param("x",x).param("y",y).param("z",z));
    }
    public ScriptContext teleport(double x, double y, double z, float yaw, float pitch) {
        return add(new StoryAction(ActionType.TELEPORT_PLAYER)
                .param("x",x).param("y",y).param("z",z).param("yaw",yaw).param("pitch",pitch));
    }

    public ScriptContext giveItem(String item, int count) {
        return add(new StoryAction(ActionType.GIVE_ITEM).param("item",item).param("count",count));
    }
    public ScriptContext giveCoins(long amount) { return add(new StoryAction(ActionType.GIVE_COINS).param("amount",amount)); }
    public ScriptContext giveXp(long amount) { return add(new StoryAction(ActionType.GIVE_XP).param("amount",amount)); }

    public ScriptContext playSound(String s) { return add(new StoryAction(ActionType.PLAY_SOUND).param("sound",s)); }
    public ScriptContext command(String c) { return add(new StoryAction(ActionType.EXECUTE_COMMAND).param("command",c)); }
    public ScriptContext setWeather(String w) { return add(new StoryAction(ActionType.SET_WEATHER).param("weather",w)); }
    public ScriptContext setTime(long t) { return add(new StoryAction(ActionType.SET_TIME).param("time",t)); }
    public ScriptContext explosion(double x, double y, double z, float p) {
        return add(new StoryAction(ActionType.EXPLOSION).param("x",x).param("y",y).param("z",z).param("power",p));
    }

    public ScriptContext freezePlayer() { return add(new StoryAction(ActionType.FREEZE_PLAYER)); }
    public ScriptContext unfreezePlayer() { return add(new StoryAction(ActionType.UNFREEZE_PLAYER)); }
    public ScriptContext startQuest(String id) { return add(new StoryAction(ActionType.START_QUEST).param("quest_id",id)); }
    public ScriptContext completeQuest(String id) { return add(new StoryAction(ActionType.COMPLETE_QUEST).param("quest_id",id)); }
    public ScriptContext setVariable(String n, String v) {
        return add(new StoryAction(ActionType.SET_VARIABLE).param("variable",n).param("value",v));
    }
    public ScriptContext changeChapter(String id) { return add(new StoryAction(ActionType.CHANGE_CHAPTER).param("chapter_id",id)); }
    public ScriptContext endStory() { return add(new StoryAction(ActionType.END_STORY)); }

    public Story build() {
        if (currentChapter != null) story.addChapter(currentChapter);
        return story;
    }

    public Story buildAndRegister() {
        Story built = build();
        StoryEngineMod.getInstance().getStoryManager().registerStory(built);
        return built;
    }
}