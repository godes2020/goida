package ru.orjus.menu.buttonAction;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import ru.orjus.menu.MenuSession;

public record OpenLinkAction(String url) implements ButtonAction {

    @Override
    public void execute(MenuSession session) {
        session.getPlayer().sendMessage(
                Component.text("Нажмите, чтобы открыть ссылку", NamedTextColor.AQUA)
                        .clickEvent(ClickEvent.openUrl(url)));
    }

}
