package ru.orjus.menu.cursor;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

public class ItemCursor implements Cursor {
    private final ItemDisplay itemDisplay;
    private final ItemStack inActiveItem;
    private final ItemStack activeItem;
    private final float scale;
    private static final float Z_THICKNESS = 0.01f;

    public ItemCursor(ItemDisplay itemDisplay, ItemStack inActiveItem, ItemStack activeItem, float scale) {
        this.itemDisplay = itemDisplay;
        this.inActiveItem = inActiveItem;
        this.activeItem = activeItem;
        this.scale = scale;
    }

    @Override
    public void setActive() {
        itemDisplay.setItemStack(activeItem);
        Transformation t = itemDisplay.getTransformation();
        itemDisplay.setTransformation(new Transformation(
                t.getTranslation(),
                t.getLeftRotation(),
                new Vector3f(-scale, scale, Z_THICKNESS),
                t.getRightRotation()));
    }

    @Override
    public void setInActive() {
        itemDisplay.setItemStack(inActiveItem);
        Transformation t = itemDisplay.getTransformation();
        itemDisplay.setTransformation(new Transformation(
                t.getTranslation(),
                t.getLeftRotation(),
                new Vector3f(-scale, scale, Z_THICKNESS),
                t.getRightRotation()));
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
