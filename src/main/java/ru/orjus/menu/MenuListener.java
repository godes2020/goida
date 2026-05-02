package ru.orjus.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.*;

public final class MenuListener implements Listener {

    private final MenuManager manager;

    public MenuListener(MenuManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        manager.openMenu(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        manager.closeMenu(e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        MenuSession s = manager.getSession(e.getPlayer());
        if (s != null) s.handleMove(e);
    }

    @EventHandler
    public void onFlight(PlayerToggleFlightEvent e) {
        if (manager.hasSession(e.getPlayer())) {
            e.setCancelled(true);
            e.getPlayer().setFlying(true);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        MenuSession s = manager.getSession(e.getPlayer());
        if (s != null) {
            e.setCancelled(true);
            s.reattachCamera();
        }
    }

    @EventHandler
    public void onDismount(org.bukkit.event.entity.EntityDismountEvent e) {
        if (e.getEntity() instanceof Player p && manager.hasSession(p)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && manager.hasSession(p)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player p && manager.hasSession(p)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (manager.hasSession(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (manager.hasSession(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (manager.hasSession(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler
    public void onAttack(PlayerInteractEntityEvent e) {
        if (manager.hasSession(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler
    public void onAnim(PlayerAnimationEvent e) {
        MenuSession s = manager.getSession(e.getPlayer());
        if (s != null) s.handleClick();
    }
}
