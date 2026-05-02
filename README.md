# OrjusMenu

Серверный плагин для Minecraft 1.21.8 — реализует кастомное 3D-меню с курсором (как у dmc-minecraft.net), фиксированной камерой и интеграцией с ItemsAdder.

## Как работает

- На вход игрок садится пассажиром на невидимую свинью (Pig с `setAI(false)`).
- Через ProtocolLib шлётся пакет `Server.CAMERA` — клиент рендерит обзор от свиньи (камера зафиксирована).
- Поскольку клиент в режиме "пассажир", он продолжает слать поворот мыши на сервер.
- В каждом тике плагин читает yaw/pitch игрока и пересчитывает позицию курсора.
- Курсор и кнопки — `TextDisplay` / `ItemDisplay` сущности с `Billboard.CENTER`.
- Хитбоксы кнопок — bounding-box проверка по экранным координатам.

## Структура проекта

- `src/main/java/ru/orjus/menu/` — исходники плагина
- `src/main/resources/config.yml` — конфиг (позиции кнопок, чувствительность курсора, FOV)
- `src/main/resources/itemsadder/` — ресурс-пак для автокопирования в `plugins/ItemsAdder/contents/orjusmenu/`
- `lib/` — Paper API, ProtocolLib, Adventure и прочие зависимости для компиляции

## Сборка

```bash
javac -d build -cp "lib/*" --release 21 -encoding UTF-8 $(find src/main/java -name '*.java')
cp -r src/main/resources/* build/
jar cf OrjusMenu-1.0.0.jar -C build .
```

## Установка

1. Положи `OrjusMenu-1.0.0.jar` в `plugins/`
2. Установи **ProtocolLib** (обязательная зависимость)
3. Опционально установи **ItemsAdder** для кастомных текстур
4. Запусти сервер — плагин автоматически перенесёт ресурсы в `plugins/ItemsAdder/contents/orjusmenu/`
5. `/iazip` + `/iareload` — клиент получит обновлённый ресурс-пак

## Возможности

- Зафиксированная камера + читаемый ввод мыши (через трюк со свиньёй и `Server.CAMERA`)
- Курсор с настраиваемой текстурой (TextDisplay / ItemDisplay / ItemsAdder)
- Кнопки с hover-эффектом и кликами
- Отдельный экран настроек FOV (10–110 шаг 1, сохраняется per-player в `playerdata.yml`)
- Уголки `┏┓┗┛` показывают границы выбранного FOV
- Все позиции масштабируются под FOV игрока
- HUD клиента (прицел, hotbar, XP) скрывается через `ClientboundGameEventPacket(CHANGE_GAME_MODE, 3)` без реального изменения геймода

## Лицензия

MIT
