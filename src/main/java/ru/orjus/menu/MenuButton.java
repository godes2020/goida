package ru.orjus.menu;

import ru.orjus.menu.buttonAction.ButtonAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

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

    private TextDisplay display;
    private boolean hovered = false;
    private boolean visible = true;
    private Location spawnLocation;

    public MenuButton(String id, String text, float screenX, float screenY,
            float halfWidth, float halfHeight, float scale,
            TextColor idleColor, TextColor hoverColor, ButtonAction action) {
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
    }

    public void spawn(Location loc) {
        this.spawnLocation = loc.clone();
        display = (TextDisplay) loc.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
        display.setBillboard(Display.Billboard.CENTER);
        display.setSeeThrough(true);
        display.setShadowed(false);
        display.setPersistent(false);
        display.setInvulnerable(true);
        display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        display.setViewRange(2.0f);
        renderText();

        Transformation t = display.getTransformation();
        display.setTransformation(new Transformation(
                new Vector3f(0f, 0f, 0f),
                t.getLeftRotation(),
                new Vector3f(scale, scale, 1f),
                t.getRightRotation()));
    }

    private void renderText() {
        if (display == null || display.isDead())
            return;
        TextColor color = hovered ? hoverColor : idleColor;
        display.text(Component.text(text).color(color).decorate(TextDecoration.BOLD));
    }

    public void setHovered(boolean hovered) {
        if (this.hovered == hovered)
            return;
        this.hovered = hovered;
        renderText();
    }

    public void setBackgroundColor(Color color) {
        if (display != null && !display.isDead())
            display.setBackgroundColor(color);
    }

    public boolean contains(float cursorX, float cursorY) {
        return visible && Math.abs(cursorX - screenX) <= halfWidth
                && Math.abs(cursorY - screenY) <= halfHeight;
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
