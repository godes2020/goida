package ru.orjus.menu.buttonAction;

import ru.orjus.menu.MenuSession;

public record ChangeSettingAction(String key, int delta, int min, int max) implements ButtonAction {

    @Override
    public void execute(MenuSession session) {
        session.changeSetting(key, delta, min, max);
    }

}
