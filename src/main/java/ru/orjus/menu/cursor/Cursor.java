package ru.orjus.menu.cursor;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;

public interface Cursor {
    public void setActive();

    public void setInActive();

    public void teleport(Location location);

    public Display getDisplay();
}