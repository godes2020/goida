package ru.orjus.menu.buttonAction;

import ru.orjus.menu.MenuSession;

public record OpenMenuAction(String screen) implements ButtonAction {

    @Override
    public void execute(MenuSession session) {
        session.loadScreen(screen);
    }

}
