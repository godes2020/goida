package ru.orjus.menu;

import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class MenuManager {

    private final MenuPlugin plugin;
    private final Map<UUID, MenuSession> sessions = new ConcurrentHashMap<>();

    public MenuManager(MenuPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean hasSession(Player p) {
        return sessions.containsKey(p.getUniqueId());
    }

    public MenuSession getSession(Player p) {
        return sessions.get(p.getUniqueId());
    }

    public void openMenu(Player p) {
        if (sessions.containsKey(p.getUniqueId())) return;
        MenuSession s = new MenuSession(plugin, p);
        sessions.put(p.getUniqueId(), s);
        s.start();
    }

    public void closeMenu(Player p) {
        MenuSession s = sessions.remove(p.getUniqueId());
        if (s != null) s.stop();
    }

    public void tickAll() {
        for (MenuSession s : sessions.values()) {
            try { s.tick(); } catch (Throwable t) { t.printStackTrace(); }
        }
    }

    public void shutdown() {
        for (MenuSession s : sessions.values()) {
            try { s.stop(); } catch (Throwable t) { t.printStackTrace(); }
        }
        sessions.clear();
    }
}
