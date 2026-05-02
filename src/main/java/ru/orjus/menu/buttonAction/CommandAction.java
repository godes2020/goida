package ru.orjus.menu.buttonAction;

import org.bukkit.Bukkit;
import ru.orjus.menu.MenuSession;

public record CommandAction(String command) implements ButtonAction {

    @Override
    public void execute(MenuSession session) {
        String cmd = command.replace("%player%", session.getPlayer().getName());
        Bukkit.getScheduler().runTask(session.getPlugin(), () ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
    }

}
