package ru.orjus.menu.cursor;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;

public class ItemCursor implements Cursor {
    private final ItemDisplay itemDisplay;
    private final ItemStack inActiveItem;
    private final ItemStack activeItem;

    public ItemCursor(ItemDisplay itemDisplay, ItemStack inActiveItem, ItemStack activeItem) {
        this.itemDisplay = itemDisplay;
        this.inActiveItem = inActiveItem;
        this.activeItem = activeItem;
    }

    @Override
    public void setActive() {
        itemDisplay.setItemStack(activeItem);
    }

    @Override
    public void setInActive() {
        itemDisplay.setItemStack(inActiveItem);
    }

    @Override
    public void teleport(Location location) {
        itemDisplay.teleport(location);
    }

    @Override
    public Display getDisplay() {
        return itemDisplay;
    }
}
