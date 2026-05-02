package ru.orjus.menu.buttonAction;

import ru.orjus.menu.MenuSession;

public sealed interface ButtonAction permits
        CommandAction,
        OpenMenuAction,
        ChangeSettingAction,
        OpenLinkAction,
        ToggleButtonAction {

    void execute(MenuSession session);

}
