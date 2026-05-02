package ru.orjus.menu.buttonAction;

import ru.orjus.menu.MenuButton;
import ru.orjus.menu.MenuSession;

public record ToggleButtonAction(String targetId) implements ButtonAction {

    @Override
    public void execute(MenuSession session) {
        for (MenuButton button : session.getButtons()) {
            if (button.id.equals(targetId)) {
                button.setVisible(!button.isVisible());
                break;
            }
        }
    }

}
