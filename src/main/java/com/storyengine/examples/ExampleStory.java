// examples/ExampleStory.java
package com.storyengine.examples;

import com.storyengine.StoryEngineMod;
import com.storyengine.core.*;
import com.storyengine.cutscene.*;
import com.storyengine.dialogue.*;
import com.storyengine.quest.*;
import com.storyengine.script.ScriptContext;

/**
 * Пример создания полного сценария через API
 */
public class ExampleStory {

    public static void register() {
        createDialogues();
        createQuests();
        createCutscenes();
        createStory();
    }

    private static void createDialogues() {
        DialogueManager dm = StoryEngineMod.getInstance().getDialogueManager();

        // Диалог со Старцем
        DialogueTree elderDialogue = new DialogueTree("elder_intro")
                .setName("Встреча со Старцем")
                .addNode(new DialogueNode("start", "§6Старец Мирон",
                        "Ах, путник... Я ждал тебя. Пророчество сбывается.")
                        .setNext("explain"))
                .addNode(new DialogueNode("explain", "§6Старец Мирон",
                        "Тёмные силы пробудились в Забытых Катакомбах. Только избранный может остановить их.")
                        .addChoice(new DialogueChoice.Builder("Я готов! Что нужно делать?")
                                .next("accept")
                                .setVar("accepted_quest", "true")
                                .build())
                        .addChoice(new DialogueChoice.Builder("Расскажи подробнее об этом зле...")
                                .next("lore")
                                .build())
                        .addChoice(new DialogueChoice.Builder("Мне не интересно. Прощай.")
                                .next("refuse")
                                .setVar("refused_elder", "true")
                                .build()))
                .addNode(new DialogueNode("lore", "§6Старец Мирон",
                        "Столетия назад здесь правил Тёмный Лорд Маракус. Он был побеждён, но его дух всё ещё бродит в катакомбах...")
                        .setNext("accept_after_lore"))
                .addNode(new DialogueNode("accept_after_lore", "§6Старец Мирон",
                        "Теперь ты знаешь правду. Поможешь ли ты нам?")
                        .addChoice(new DialogueChoice.Builder("Конечно! Я уничтожу зло!")
                                .next("accept")
                                .setVar("accepted_quest", "true")
                                .build())
                        .addChoice(new DialogueChoice.Builder("Мне нужно подумать...")
                                .next("maybe")
                                .build()))
                .addNode(new DialogueNode("accept", "§6Старец Мирон",
                        "§aОтлично! Возьми этот меч — он был выкован специально для борьбы с тьмой. Катакомбы находятся на востоке, за Тёмным лесом.")
                        .setAction("start_quest:catacombs_quest"))
                .addNode(new DialogueNode("refuse", "§6Старец Мирон",
                        "§7Жаль... Может быть, ты передумаешь. Я буду ждать здесь."))
                .addNode(new DialogueNode("maybe", "§6Старец Мирон",
                        "§eЯ понимаю. Когда будешь готов — приходи ко мне."))
                .onComplete("advance_story");

        dm.registerDialogue(elderDialogue);

        // Диалог в катакомбах
        DialogueTree catacombDialogue = new DialogueTree("catacomb_entrance")
                .setName("Вход в катакомбы")
                .addNode(new DialogueNode("start", "§4???",
                        "§cТы... осмелился прийти сюда? Глупец...")
                        .setNext("threat"))
                .addNode(new DialogueNode("threat", "§4Голос из тьмы",
                        "§cМаракус ждёт тебя... Но ты не переживёшь и первого зала. ХАХАХА!")
                        .addChoice(new DialogueChoice.Builder("§eМне не страшно! [Войти]")
                                .next("enter").build())
                        .addChoice(new DialogueChoice.Builder("§7[Отступить]")
                                .next("retreat").build()))
                .addNode(new DialogueNode("enter", "§4Голос из тьмы",
                        "§cТогда войди... и умри."))
                .addNode(new DialogueNode("retreat", "§4Голос из тьмы",
                        "§cХА! Трус! Беги, пока можешь!"));

        dm.registerDialogue(catacombDialogue);
    }

    private static void createQuests() {
        QuestManager qm = StoryEngineMod.getInstance().getQuestManager();

        Quest catacombsQuest = new Quest("catacombs_quest", "§4Очищение Катакомб")
                .setDescription("Отправляйтесь в Забытые Катакомбы и уничтожьте Тёмного Лорда Маракуса")
                .addObjective(new QuestObjective("reach_catacombs",
                        QuestObjective.ObjectiveType.REACH_LOCATION, "catacombs_entrance",
                        1, "Найти вход в Катакомбы"))
                .addObjective(new QuestObjective("kill_undead",
                        QuestObjective.ObjectiveType.KILL_ENTITY, "minecraft:zombie",
                        20, "Уничтожить нежить (0/20)"))
                .addObjective(new QuestObjective("kill_skeletons",
                        QuestObjective.ObjectiveType.KILL_ENTITY, "minecraft:skeleton",
                        10, "Уничтожить скелетов-стражей (0/10)"))
                .addObjective(new QuestObjective("defeat_boss",
                        QuestObjective.ObjectiveType.KILL_ENTITY, "minecraft:wither_skeleton",
                        1, "Победить Маракуса"))
                .addReward("minecraft:diamond_sword", 1)
                .addReward("minecraft:golden_apple", 5)
                .addReward("minecraft:experience_bottle", 20)
                .onComplete("advance_story");

        qm.registerQuest(catacombsQuest);
    }

    private static void createCutscenes() {
        CutsceneManager cm = StoryEngineMod.getInstance().getCutsceneManager();

        // Вступительная катсцена
        CameraPath introPath = new CameraPath()
                .addPoint(100, 120, 200, 0, 45, 80)    // Вид сверху
                .addPoint(100, 80, 180, 0, 15, 60)     // Приближение
                .addPoint(105, 66, 165, -15, 5, 40)    // К NPC
                .addPoint(102, 65, 160, -5, 0, 20);    // Финал

        Cutscene introCutscene = new Cutscene("intro_flyover")
                .setName("Обзор деревни")
                .setCameraPath(introPath)
                .setHideHUD(true)
                .setLetterbox(true)
                .addTimedAction(0, new StoryAction(ActionType.SHOW_TITLE)
                        .param("text", "§6§lЗабытые Земли")
                        .param("fade_in", 20).param("stay", 60).param("fade_out", 20))
                .addTimedAction(20, new StoryAction(ActionType.SHOW_SUBTITLE)
                        .param("text", "§7Глава I: Пробуждение"))
                .addTimedAction(100, new StoryAction(ActionType.PLAY_SOUND)
                        .param("sound", "minecraft:ambient.cave")
                        .param("volume", 0.5).param("pitch", 0.8));

        cm.registerCutscene(introCutscene);
    }

    private static void createStory() {
        // Используем Fluent API
        ScriptContext.createStory("forgotten_lands", "Забытые Земли")
                .author("StoryEngine")
                .description("Эпическое приключение в мире тьмы и света")

                // === Глава 1: Пробуждение ===
                .chapter("ch1_awakening", "§6Глава 1: Пробуждение")

                // Затемнение при начале
                .fadeOut(1)
                .wait(5)

                // Телепорт к начальной точке
                .teleport(100, 64, 200, 0, 0)
                .wait(10)

                // Изменить время на утро
                .setTime(1000)
                .setWeather("clear")

                // Катсцена обзора
                .cutscene("intro_flyover")
                .wait(200)  // Ждём окончания катсцены

                // Появление
                .fadeIn(20)
                .wait(30)

                // Спавн NPC
                .spawnNPC("elder", "Старец Мирон", 102, 64, 158)
                .wait(20)

                // NPC говорит
                .npcSay("Старец Мирон", "О! Ты наконец очнулся, путник!")
                .wait(60)

                // Начало диалога
                .dialogue("elder_intro")

                .endChapter()

                // === Глава 2: Путь к Катакомбам ===
                .chapter("ch2_journey", "§6Глава 2: Путь")

                // Дать предмет
                .giveItem("minecraft:iron_sword", 1)
                .giveItem("minecraft:bread", 10)
                .wait(20)

                .title("§6Глава 2", 10, 40, 10)
                .subtitle("§7Путь к Катакомбам")
                .wait(60)

                .npcSay("Старец Мирон", "Иди на восток через лес. Будь осторожен!")
                .wait(40)

                // NPC уходит
                .moveNPC("elder", 90, 64, 158)
                .wait(100)
                .removeNPC("elder")

                // Начать квест
                .startQuest("catacombs_quest")

                .endChapter()

                // === Глава 3: Катакомбы ===
                .chapter("ch3_catacombs", "§4Глава 3: Катакомбы")

                .fadeOut(10)
                .wait(20)
                .teleport(500, 30, 200)
                .fadeIn(10)
                .wait(30)

                .title("§4Глава 3", 10, 40, 10)
                .subtitle("§cЗабытые Катакомбы")
                .wait(60)

                .setTime(18000) // Ночь
                .setWeather("thunder")

                .dialogue("catacomb_entrance")
                .wait(40)

                // Спавн врагов через команду
                .command("summon minecraft:zombie 505 30 200")
                .command("summon minecraft:zombie 510 30 205")
                .command("summon minecraft:skeleton 508 30 195")

                .playSound("minecraft:entity.wither.ambient")

                .endChapter()

                // === Глава 4: Финал ===
                .chapter("ch4_finale", "§e§lФинал")

                .freezePlayer()
                .fadeOut(20)
                .wait(40)

                .title("§e§lПобеда!", 20, 60, 20)
                .subtitle("§7Маракус повержен. Мир спасён.")
                .wait(100)

                .setWeather("clear")
                .setTime(6000)

                .fadeIn(20)
                .unfreezePlayer()
                .wait(20)

                // Появление старца
                .spawnNPC("elder_final", "Старец Мирон", 500, 64, 200)
                .wait(40)

                .npcSay("Старец Мирон", "§6Ты сделал это, герой! Мир спасён благодаря тебе!")
                .wait(60)

                .giveItem("minecraft:nether_star", 1)
                .giveItem("minecraft:diamond", 64)

                .npcSay("Старец Мирон", "§6Прими эти дары в знак благодарности.")
                .wait(60)

                // Завершение
                .title("§6§l✦ КОНЕЦ ✦", 20, 100, 40)
                .subtitle("§7Спасибо за прохождение!")
                .wait(160)

                .endStory()

                .endChapter()

                .buildAndRegister();
    }
}