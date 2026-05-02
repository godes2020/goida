package ru.orjus.menu;

import ru.orjus.menu.buttonAction.ButtonAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.List;

public final class MenuButton {

    public final String id;
    public final String text;
    public final float screenX;
    public final float screenY;
    public final float halfWidth;
    public final float halfHeight;
    public final float scale;
    public final ButtonAction action;
    public final TextColor idleColor;
    public final TextColor hoverColor;

    private final ItemStack idleItem;
    private final ItemStack hoverItem;
    private final boolean isItemButton;

    private Display display;
    private boolean hovered = false;
    private boolean visible = true;
    private Location spawnLocation;
    private MenuButton parent;
    private List<MenuButton> children;
    private float hoverRadius = 0.35f;

    public MenuButton(String id, String text, float screenX, float screenY,
            float halfWidth, float halfHeight, float scale,
            TextColor idleColor, TextColor hoverColor, ButtonAction action) {
        this(id, text, screenX, screenY, halfWidth, halfHeight, scale,
                idleColor, hoverColor, action, null, null);
    }

    public MenuButton(String id, String text, float screenX, float screenY,
            float halfWidth, float halfHeight, float scale,
            TextColor idleColor, TextColor hoverColor, ButtonAction action,
            ItemStack idleItem, ItemStack hoverItem) {
        this.id = id;
        this.text = text;
        this.screenX = screenX;
        this.screenY = screenY;
        this.halfWidth = halfWidth;
        this.halfHeight = halfHeight;
        this.scale = scale;
        this.idleColor = idleColor;
        this.hoverColor = hoverColor;
        this.action = action;
        this.idleItem = idleItem;
        this.hoverItem = hoverItem;
        this.isItemButton = (idleItem != null);
    }

    public void setParent(MenuButton parent) {
        this.parent = parent;
    }

    public MenuButton getParent() {
        return parent;
    }

    public void setChildren(List<MenuButton> children) {
        this.children = children;
    }

    public List<MenuButton> getChildren() {
        return children;
    }

    public void setHoverRadius(float hoverRadius) {
        this.hoverRadius = hoverRadius;
    }

    public void spawn(Location loc) {
        this.spawnLocation = loc.clone();
        if (isItemButton) {
            spawnItemDisplay(loc);
        } else {
            spawnTextDisplay(loc);
        }
    }

    private void spawnTextDisplay(Location loc) {
        TextDisplay td = (TextDisplay) loc.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
        td.setBillboard(Display.Billboard.CENTER);
        td.setSeeThrough(true);
        td.setShadowed(false);
        td.setPersistent(false);
        td.setInvulnerable(true);
        td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        td.setViewRange(2.0f);
        display = td;
        renderText();

        Transformation t = td.getTransformation();
        td.setTransformation(new Transformation(
                new Vector3f(0f, 0f, 0f),
                t.getLeftRotation(),
                new Vector3f(scale, scale, 1f),
                t.getRightRotation()));
    }

    private void spawnItemDisplay(Location loc) {
        ItemDisplay itemDisplay = loc.getWorld().spawn(loc, ItemDisplay.class, disp -> {
            disp.setBillboard(Display.Billboard.CENTER);
            disp.setPersistent(false);
            disp.setInvulnerable(true);
            disp.setItemStack(hovered ? hoverItem : idleItem);
            disp.setViewRange(10f);
            disp.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI);
            disp.setTeleportDuration(2);
            disp.setBrightness(new Display.Brightness(15, 15));

            Transformation t = disp.getTransformation();
            disp.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    t.getLeftRotation(),
                    new Vector3f(scale, scale, 0.01f),
                    t.getRightRotation()));
        });
        display = itemDisplay;
    }

    private void renderText() {
        if (!(display instanceof TextDisplay td) || td.isDead())
            return;
        TextColor color = hovered ? hoverColor : idleColor;
        td.text(Component.text(text).color(color).decorate(TextDecoration.BOLD));
    }

    public void setHovered(boolean hovered) {
        if (this.hovered == hovered)
            return;
        this.hovered = hovered;
        if (isItemButton) {
            updateItemDisplay();
        } else {
            renderText();
        }
        if (children != null) {
            for (MenuButton child : children) {
                child.setVisible(hovered);
            }
        }
    }

    private void updateItemDisplay() {
        if (display instanceof ItemDisplay itemDisplay && !itemDisplay.isDead()) {
            itemDisplay.setItemStack(hovered ? hoverItem : idleItem);
        }
    }

    public void setBackgroundColor(Color color) {
        if (display instanceof TextDisplay td && !td.isDead())
            td.setBackgroundColor(color);
    }

    public boolean contains(float cursorX, float cursorY) {
        if (!visible)
            return false;
        if (Math.abs(cursorX - screenX) <= halfWidth
                && Math.abs(cursorY - screenY) <= halfHeight)
            return true;
        if (children != null) {
            for (MenuButton child : children) {
                if (child.isVisible() && child.contains(cursorX, cursorY))
                    return true;
            }
            float minX = screenX - halfWidth;
            float maxX = screenX + halfWidth;
            float minY = screenY - halfHeight;
            float maxY = screenY + halfHeight;
            boolean anyVisible = false;
            for (MenuButton child : children) {
                if (child.isVisible()) {
                    anyVisible = true;
                    minX = Math.min(minX, child.screenX - child.halfWidth);
                    maxX = Math.max(maxX, child.screenX + child.halfWidth);
                    minY = Math.min(minY, child.screenY - child.halfHeight);
                    maxY = Math.max(maxY, child.screenY + child.halfHeight);
                }
            }
            if (anyVisible) {
                if (cursorX >= minX - hoverRadius && cursorX <= maxX + hoverRadius
                        && cursorY >= minY - hoverRadius && cursorY <= maxY + hoverRadius)
                    return true;
            }
        }
        return false;
    }

    public void remove() {
        if (display != null && !display.isDead())
            display.remove();
        display = null;
        visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        if (this.visible == visible)
            return;
        this.visible = visible;
        if (visible && spawnLocation != null) {
            spawn(spawnLocation);
        } else {
            remove();
        }
    }

}
