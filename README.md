# StoryEngine
StoryEngine это - движок сюжетов для Minecraft 1.20.1 (Forge). Создавайте интерактивные истории с NPC, диалогами, катсценами и квестами прямо в игре или через JSON-файлы.  Вдохновлён модом Hollow Engine.

## 📋 Требования

- Minecraft 1.20.1
- Forge 47.2.0+
- Java 17

## 📥 Установка

1. Скачайте `storyengine-1.0.0.jar`
2. Поместите в папку `mods/`
3. Запустите игру


## ✨ Возможности

- 👤 **NPC** — создание, перемещение, скины, привязка диалогов
- 💬 **Диалоги** — ветвящиеся деревья с выборами и условиями
- 🎬 **Катсцены** — камера по точкам, letterbox, fade-эффекты
- 📜 **Квесты** — цели, награды, автозавершение, цепочки
- 📖 **Истории** — главы, действия, задержки, переменные
- 📝 **Скрипты** — JSON и текстовые (.script)
- 🔧 **Команды** — полное управление из чата

## 🚀 Быстрый старт

/npc spawn guide "Проводник" ~ ~ ~3
/npc dialogue guide example_dialogue
/dialogue start example_dialogue


## 💻 Команды

### /story

| Команда | Описание |
|---------|----------|
| `/story start <id> [player]` | Запустить историю |
| `/story stop [player]` | Остановить историю |
| `/story chapter <id> [player]` | Перейти к главе |
| `/story list` | Список историй |
| `/story reload` | Перезагрузить данные |
| `/story save` | Сохранить истории |
| `/story create <id> <name>` | Создать историю |
| `/story variable set <name> <value>` | Установить переменную |
| `/story variable get <name>` | Посмотреть переменную |
| `/story path` | Пути к файлам |
| `/story status [player]` | Статус игрока |
| `/story debug` | Отладка |

### /npc

| Команда | Описание |
|---------|----------|
| `/npc spawn <id> <name> [x y z] [skin]` | Создать NPC |
| `/npc remove <id>` | Удалить NPC |
| `/npc removeall` | Удалить всех |
| `/npc move <id> <x y z> [speed]` | Переместить |
| `/npc dialogue <id> <dialogue_id>` | Привязать диалог |
| `/npc list` | Список NPC |

### /cutscene

| Команда | Описание |
|---------|----------|
| `/cutscene play <id> [player]` | Запустить |
| `/cutscene stop` | Остановить |
| `/cutscene create <id> <name>` | Начать создание |
| `/cutscene addpoint [ticks]` | Добавить точку |
| `/cutscene finish` | Завершить |
| `/cutscene list` | Список |

### /dialogue

| Команда | Описание |
|---------|----------|
| `/dialogue start <id> [player]` | Открыть диалог |
| `/dialogue stop` | Закрыть |
| `/dialogue list` | Список |


## 📖 Примеры

### История (JSON)
`
{
  "id": "demo",
  "name": "Демо",
  "author": "Author",
  "startChapter": "intro",
  "chapters": [
    {
      "id": "intro",
      "name": "Введение",
      "autoAdvance": false,
      "actions": [
        { "type": "FADE_OUT", "delay": 0, "parameters": { "duration": 5 } },
        { "type": "TELEPORT_PLAYER", "delay": 5, "parameters": { "x": 100, "y": 64, "z": 100 } },
        { "type": "FADE_IN", "delay": 3, "parameters": { "duration": 20 } },
        {
          "type": "SHOW_TITLE", "delay": 0,
          "parameters": { "text": "§6§lДемо", "fade_in": 10, "stay": 40, "fade_out": 10 }
        },
        {
          "type": "SPAWN_NPC", "delay": 20,
          "parameters": {
            "npc_id": "guide", "name": "Проводник",
            "x": 103, "y": 64, "z": 100,
            "dialogue": "example_dialogue"
          }
        }
      ]
    }
  ]
}
`
### Диалог (JSON)
`
{
  "id": "example_dialogue",
  "name": "Пример",
  "startNode": "start",
  "nodes": [
    {
      "id": "start",
      "speaker": "Проводник",
      "text": "Привет! Готов к приключению?",
      "choices": [
        { "text": "Да!", "next": "accept" },
        { "text": "Нет", "next": "refuse" }
      ]
    },
    { "id": "accept", "speaker": "Проводник", "text": "Тогда вперёд!" },
    { "id": "refuse", "speaker": "Проводник", "text": "Возвращайся когда будешь готов." }
  ]
}
`
### Квест (JSON)
`
{
  "id": "kill_zombies",
  "name": "Охота",
  "description": "Убейте зомби",
  "autoComplete": true,
  "objectives": [
    {
      "id": "kill", "type": "KILL_ENTITY",
      "target": "minecraft:zombie", "count": 5,
      "description": "Убить зомби"
    }
  ],
  "rewards": ["minecraft:diamond:3"],
  "onComplete": "chapter:ch2"
}
`
### Текстовый скрипт (.script)

# Файл: storyengine/scripts/test.script
`0 FREEZE_PLAYER
0 FADE_OUT duration=5
5 TELEPORT_PLAYER x=0 y=64 z=0
8 FADE_IN duration=20
8 SHOW_TITLE text="§6§lПривет!" fade_in=10 stay=40 fade_out=10
40 UNFREEZE_PLAYER
45 SPAWN_NPC npc_id=test name=Тест x=3 y=64 z=0 dialogue=example_dialogue
100 END_STORY`

## ⚡ Типы действий

### Сюжет

| Тип | Параметры |
|-----|-----------|
| `DIALOGUE` | `dialogue_id` |
| `CUTSCENE` | `cutscene_id` |
| `CHANGE_CHAPTER` | `chapter_id` |
| `END_STORY` | — |

### NPC

| Тип | Параметры |
|-----|-----------|
| `SPAWN_NPC` | `npc_id`, `name`, `x`, `y`, `z`, `skin`, `dialogue` |
| `REMOVE_NPC` | `npc_id` |
| `MOVE_NPC` | `npc_id`, `x`, `y`, `z`, `speed` |
| `NPC_SAY` | `npc_name`, `message`, `color` |

### Игрок

| Тип | Параметры |
|-----|-----------|
| `TELEPORT_PLAYER` | `x`, `y`, `z`, `yaw`, `pitch` |
| `GIVE_ITEM` | `item`, `count` |
| `FREEZE_PLAYER` | — |
| `UNFREEZE_PLAYER` | — |

### Мир

| Тип | Параметры |
|-----|-----------|
| `SET_BLOCK` | `pos_x`, `pos_y`, `pos_z`, `block` |
| `FILL_BLOCKS` | `from_x/y/z`, `to_x/y/z`, `block` |
| `SET_WEATHER` | `weather`, `duration` |
| `SET_TIME` | `time` |
| `EXPLOSION` | `x`, `y`, `z`, `power`, `fire` |
| `PARTICLE` | `x`, `y`, `z`, `count` |

### Визуал

| Тип | Параметры |
|-----|-----------|
| `SHOW_TITLE` | `text`, `fade_in`, `stay`, `fade_out` |
| `SHOW_SUBTITLE` | `text` |
| `PLAY_SOUND` | `sound`, `volume`, `pitch` |
| `FADE_IN` | `duration` |
| `FADE_OUT` | `duration` |

### Квесты

| Тип | Параметры |
|-----|-----------|
| `START_QUEST` | `quest_id` |
| `COMPLETE_QUEST` | `quest_id` |
| `SET_VARIABLE` | `variable`, `value` |
| `EXECUTE_COMMAND` | `command` |

## 🔀 Условия

| Формат | Пример |
|--------|--------|
| `var==value` | `accepted_quest==true` |
| `var!=value` | `refused==true` |
| `var>=number` | `reputation>=50` |
| `var<=number` | `reputation<=10` |

## 🎮 Управление

| Клавиша | Действие |
|---------|----------|
| ПКМ на NPC | Открыть диалог |
| ЛКМ / Пробел / Enter | Продвинуть диалог |
| 1–9 | Выбрать вариант ответа |
| ESC | Закрыть диалог / пропустить катсцену |

## 🔧 Fluent API

`ScriptContext.createStory("my_story", "Моя история")
    .author("Author")
    .chapter("intro", "Введение")
        .fadeOut(5)
        .teleport(100, 64, 200)
        .fadeIn(20)
        .title("§6Начало", 10, 40, 10)
        .spawnNPC("guide", "Проводник", 103, 64, 200)
        .dialogue("my_dialogue")
    .endChapter()
    .buildAndRegister();`

## 📊 Справочник

| Тики | Секунды |
|------|---------|
| 20 | 1 |
| 60 | 3 |
| 100 | 5 |
| 200 | 10 |

| Время | Значение |
|-------|----------|
| 06:00 | 0 |
| 12:00 | 6000 |
| 18:00 | 12000 |
| 00:00 | 18000 |

| Скорость NPC | Описание |
|-------------|----------|
| 0.3 | Прогулка |
| 0.7 | Ходьба |
| 1.0 | Быстро |
| 1.5 | Бег |
## 📄 Лицензия

MIT
