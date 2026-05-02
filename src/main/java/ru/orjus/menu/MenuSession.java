package ru.orjus.menu;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import ru.orjus.menu.cursor.Cursor;
import ru.orjus.menu.cursor.ItemCursor;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MenuSession {

    private final MenuPlugin plugin;
    private final Player player;

    private Location anchor;
    private float anchorYaw;
    private float anchorPitch;

    private Pig cameraEntity;
    private Cursor cursor;
    private final List<MenuButton> buttons = new ArrayList<>();
    private final List<TextDisplay> decorations = new ArrayList<>();
    private final java.util.HashMap<String, Integer> playerSettings = new java.util.HashMap<>();
    private TextDisplay fovValueDisplay;
    private String currentScreen = "main";
    private MenuButton currentHover = null;

    private float logicalYaw = 0f;
    private float lastRawYaw = 0f;
    private float logicalPitch = 0f;
    private float lastRawPitch = 0f;
    private boolean rotInitialized = false;

    private float distance = 3.0f;
    private float halfW = 2.85f;
    private float halfH = 1.6f;
    private float baseHalfW = 2.85f;
    private float baseHalfH = 1.6f;
    private float yawDegPerEdge = 50f;
    private float pitchDegPerEdge = 32f;
    private float cursorScale = 0.55f;
    private static final int BASE_FOV = 90;

    private GameMode previousGameMode;
    private boolean previousFlying;
    private boolean previousAllowFlight;

    private long debugCounter = 0;

    public MenuSession(MenuPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void start() {
        ConfigurationSection cfg = plugin.getConfig();
        World world = Bukkit.getWorld(cfg.getString("menu.world", "world"));
        if (world == null)
            world = player.getWorld();

        anchorYaw = (float) cfg.getDouble("menu.yaw", 0.0);
        anchorPitch = (float) cfg.getDouble("menu.pitch", 0.0);
        anchor = new Location(world,
                cfg.getDouble("menu.x", 0.5),
                cfg.getDouble("menu.y", 150.0),
                cfg.getDouble("menu.z", 0.5),
                anchorYaw, anchorPitch);

        distance = (float) cfg.getDouble("cursor.distance", 3.0);
        baseHalfW = (float) cfg.getDouble("cursor.half-width", 2.85);
        baseHalfH = (float) cfg.getDouble("cursor.half-height", 1.6);
        halfW = baseHalfW;
        halfH = baseHalfH;
        yawDegPerEdge = (float) cfg.getDouble("cursor.yaw-degrees-per-edge", 50.0);
        pitchDegPerEdge = (float) cfg.getDouble("cursor.pitch-degrees-per-edge", 32.0);
        cursorScale = (float) cfg.getDouble("cursor.scale", 0.55);

        previousGameMode = player.getGameMode();
        previousFlying = player.isFlying();
        previousAllowFlight = player.getAllowFlight();

        anchor.getChunk().load(true);

        player.getInventory().clear();
        player.teleport(anchor);
        player.setGameMode(GameMode.ADVENTURE);

        try {
            PotionEffectType invis = PotionEffectType.INVISIBILITY;
            if (invis != null) {
                player.addPotionEffect(new PotionEffect(invis, Integer.MAX_VALUE, 0, false, false, false));
            }
        } catch (Throwable ignored) {
        }
        try {
            player.setInvisible(true);
        } catch (Throwable ignored) {
        }
        try {
            player.setCollidable(false);
        } catch (Throwable ignored) {
        }

        try {
            player.setLevel(0);
        } catch (Throwable ignored) {
        }
        try {
            player.setExp(0f);
        } catch (Throwable ignored) {
        }

        spawnCameraPig();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (cameraEntity == null || cameraEntity.isDead() || !player.isOnline())
                return;

            player.teleport(anchor);

            spawnCursor();
            int savedFov = plugin.getPlayerSetting(player.getUniqueId(), "fov", BASE_FOV);
            playerSettings.put("fov", savedFov);
            applyFovScale();
            loadScreen("main");
            cameraEntity.addPassenger(player);
            sendCameraPacket(cameraEntity.getEntityId());
            sendFakeGamemode(3);

            cursor.teleport(computeLocAt(0f, 0f, distance + CURSOR_Z_OFFSET));
        }, 5L);

        player.sendActionBar(Component.text("Двигайте мышь — управляйте курсором", NamedTextColor.GRAY)
                .decorate(TextDecoration.ITALIC));
    }

    public void stop() {
        if (player.isOnline()) {
            if (cameraEntity != null && cameraEntity.getPassengers().contains(player)) {
                cameraEntity.removePassenger(player);
            }
            sendCameraPacket(player.getEntityId());
            int gmId = previousGameMode == GameMode.SURVIVAL ? 0
                    : previousGameMode == GameMode.CREATIVE ? 1
                            : previousGameMode == GameMode.ADVENTURE ? 2
                                    : previousGameMode == GameMode.SPECTATOR ? 3 : 0;
            sendFakeGamemode(gmId);
            if (previousGameMode != null)
                player.setGameMode(previousGameMode);
            player.setAllowFlight(previousAllowFlight);
            player.setFlying(previousFlying);
            try {
                player.setInvisible(false);
            } catch (Throwable ignored) {
            }
            try {
                player.setCollidable(true);
            } catch (Throwable ignored) {
            }
            try {
                PotionEffectType invis = PotionEffectType.INVISIBILITY;
                if (invis != null && player.hasPotionEffect(invis))
                    player.removePotionEffect(invis);
            } catch (Throwable ignored) {
            }
        }
        if (cursor != null && !cursor.getDisplay().isDead())
            cursor.getDisplay().remove();
        if (cameraEntity != null && !cameraEntity.isDead())
            cameraEntity.remove();
        clearScreenElements();
        cursor = null;
        cameraEntity = null;
    }

    private void clearScreenElements() {
        for (MenuButton b : buttons)
            b.remove();
        buttons.clear();
        for (TextDisplay td : decorations) {
            if (td != null && !td.isDead())
                td.remove();
        }
        decorations.clear();
        if (fovValueDisplay != null && !fovValueDisplay.isDead())
            fovValueDisplay.remove();
        fovValueDisplay = null;
        currentHover = null;
    }

    private void loadScreen(String name) {
        clearScreenElements();
        currentScreen = name;
        if (name.equals("main")) {
            spawnButtons();
        } else if (name.equals("settings")) {
            spawnSettingsScreen();
        }
    }

    private TextDisplay spawnDecorationText(String text, float sx, float sy, float scale,
            TextColor color, boolean bold,
            org.bukkit.Color bgColor) {
        Location loc = computeCursorLoc(sx, sy);
        TextDisplay td = (TextDisplay) loc.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
        td.setBillboard(Display.Billboard.CENTER);
        td.setSeeThrough(true);
        td.setShadowed(false);
        td.setPersistent(false);
        td.setInvulnerable(true);
        td.setBackgroundColor(bgColor);
        td.setViewRange(2.0f);

        Component comp = Component.text(text).color(color);
        if (bold)
            comp = comp.decorate(TextDecoration.BOLD);
        td.text(comp);

        Transformation t = td.getTransformation();
        td.setTransformation(new Transformation(
                new Vector3f(0f, 0f, 0f),
                t.getLeftRotation(),
                new Vector3f(scale, scale, 1f),
                t.getRightRotation()));
        decorations.add(td);
        return td;
    }

    private final List<TextDisplay> fovBrackets = new ArrayList<>();

    private void spawnSettingsScreen() {
        org.bukkit.Color transparent = org.bukkit.Color.fromARGB(0, 0, 0, 0);
        org.bukkit.Color panelBg = org.bukkit.Color.fromARGB(230, 5, 5, 5);
        org.bukkit.Color orangeBg = org.bukkit.Color.fromARGB(255, 255, 145, 0);
        TextColor white = NamedTextColor.WHITE;
        TextColor orange = TextColor.fromHexString("#FFAA00");

        Component panelText = Component.text("Масштаб интерфейса", orange, TextDecoration.BOLD)
                .append(Component.text("\n\n", white).decoration(TextDecoration.BOLD, false))
                .append(Component.text("Пожалуйста, укажите ваш FOV.\n", white).decoration(TextDecoration.BOLD, false))
                .append(Component.text("Узнать его можно в главных\n", white).decoration(TextDecoration.BOLD, false))
                .append(Component.text("настройках игры\n\n", white).decoration(TextDecoration.BOLD, false))
                .append(Component.text("Ориентируйтесь по оранжевым\n", white).decoration(TextDecoration.BOLD, false))
                .append(Component.text("уголкам по краям экрана", white).decoration(TextDecoration.BOLD, false));

        Location panelLoc = computeCursorLoc(0f, 0.70f);
        TextDisplay panel = (TextDisplay) panelLoc.getWorld().spawnEntity(panelLoc, EntityType.TEXT_DISPLAY);
        panel.setBillboard(Display.Billboard.CENTER);
        panel.setSeeThrough(true);
        panel.setShadowed(false);
        panel.setPersistent(false);
        panel.setInvulnerable(true);
        panel.setBackgroundColor(panelBg);
        panel.setViewRange(2.0f);
        panel.setAlignment(TextDisplay.TextAlignment.CENTER);
        panel.text(panelText);
        Transformation pt = panel.getTransformation();
        panel.setTransformation(new Transformation(
                new Vector3f(0f, 0f, 0f),
                pt.getLeftRotation(),
                new Vector3f(1.1f, 1.1f, 1f),
                pt.getRightRotation()));
        decorations.add(panel);

        spawnDecorationText("Задайте FOV", 0f, 0.10f, 1.0f,
                NamedTextColor.GRAY, false, transparent);

        float arrowDx = 0.85f;
        float fovRowY = -0.50f;

        MenuButton dec = new MenuButton("fov_dec", "◄", -arrowDx, fovRowY,
                0.22f, 0.18f, 1.8f, orange, white, "change:fov:-1:70:110");
        dec.spawn(computeCursorLoc(-arrowDx, fovRowY));
        buttons.add(dec);

        int fov = playerSettings.getOrDefault("fov", BASE_FOV);
        Location valueLoc = computeCursorLoc(0f, fovRowY);
        fovValueDisplay = (TextDisplay) valueLoc.getWorld().spawnEntity(valueLoc, EntityType.TEXT_DISPLAY);
        fovValueDisplay.setBillboard(Display.Billboard.CENTER);
        fovValueDisplay.setSeeThrough(true);
        fovValueDisplay.setShadowed(false);
        fovValueDisplay.setPersistent(false);
        fovValueDisplay.setInvulnerable(true);
        fovValueDisplay.setBackgroundColor(transparent);
        fovValueDisplay.setViewRange(2.0f);
        fovValueDisplay.text(Component.text(String.valueOf(fov)).color(orange).decorate(TextDecoration.BOLD));
        Transformation tt = fovValueDisplay.getTransformation();
        fovValueDisplay.setTransformation(new Transformation(
                new Vector3f(0f, 0f, 0f),
                tt.getLeftRotation(),
                new Vector3f(2.0f, 2.0f, 1f),
                tt.getRightRotation()));

        MenuButton inc = new MenuButton("fov_inc", "►", arrowDx, fovRowY,
                0.22f, 0.18f, 1.8f, orange, white, "change:fov:1:70:110");
        inc.spawn(computeCursorLoc(arrowDx, fovRowY));
        buttons.add(inc);

        MenuButton cont = new MenuButton("continue", "Продолжить", 0f, -1.15f,
                1.10f, 0.18f, 1.5f,
                TextColor.fromHexString("#1A1A1A"),
                TextColor.fromHexString("#FFFFFF"),
                "open:main");
        cont.spawn(computeCursorLoc(0f, -1.15f));
        cont.setBackgroundColor(orangeBg);
        buttons.add(cont);

        spawnFovBrackets();
    }

    private void spawnFovBrackets() {
        for (TextDisplay td : fovBrackets) {
            if (td != null && !td.isDead())
                td.remove();
        }
        fovBrackets.clear();

        int fov = playerSettings.getOrDefault("fov", 90);
        float[] xy = computeBracketPos(fov);
        String[] chars = { "┏", "┓", "┗", "┛" };
        float[][] positions = {
                { -xy[0], xy[1] },
                { xy[0], xy[1] },
                { -xy[0], -xy[1] },
                { xy[0], -xy[1] }
        };

        TextColor orange = TextColor.fromHexString("#FFAA00");
        for (int i = 0; i < 4; i++) {
            Location loc = computeCursorLoc(positions[i][0], positions[i][1]);
            TextDisplay td = (TextDisplay) loc.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
            td.setBillboard(Display.Billboard.CENTER);
            td.setSeeThrough(true);
            td.setShadowed(false);
            td.setPersistent(false);
            td.setInvulnerable(true);
            td.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            td.setViewRange(2.0f);
            td.text(Component.text(chars[i]).color(orange).decorate(TextDecoration.BOLD));
            Transformation t = td.getTransformation();
            td.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    t.getLeftRotation(),
                    new Vector3f(3.0f, 3.0f, 1f),
                    t.getRightRotation()));
            fovBrackets.add(td);
            decorations.add(td);
        }
    }

    private void updateFovBrackets() {
        if (fovBrackets.size() != 4)
            return;
        int fov = playerSettings.getOrDefault("fov", 90);
        float[] xy = computeBracketPos(fov);
        float[][] positions = {
                { -xy[0], xy[1] },
                { xy[0], xy[1] },
                { -xy[0], -xy[1] },
                { xy[0], -xy[1] }
        };
        for (int i = 0; i < 4; i++) {
            TextDisplay td = fovBrackets.get(i);
            if (td != null && !td.isDead()) {
                td.teleport(computeCursorLoc(positions[i][0], positions[i][1]));
            }
        }
    }

    private static final double ASPECT = 16.0 / 9.0;

    private float[] computeBracketPos(int fov) {
        double vertHalfRad = Math.toRadians(fov / 2.0);
        double sy = distance * Math.tan(vertHalfRad);
        double sx = sy * ASPECT;
        return new float[] { (float) sx, (float) sy };
    }

    private float fovScale() {
        int fov = playerSettings.getOrDefault("fov", BASE_FOV);
        double userHalfTan = Math.tan(Math.toRadians(fov / 2.0));
        double baseHalfTan = Math.tan(Math.toRadians(BASE_FOV / 2.0));
        return (float) (userHalfTan / baseHalfTan);
    }

    private static final float CURSOR_INSET = 0.95f;

    private void applyFovScale() {
        int fov = playerSettings.getOrDefault("fov", BASE_FOV);
        double vertHalfRad = Math.toRadians(fov / 2.0);
        double sy = distance * Math.tan(vertHalfRad);
        double sx = sy * ASPECT;
        halfH = (float) (sy * CURSOR_INSET);
        halfW = (float) (sx * CURSOR_INSET);
    }

    private void updateFovDisplay() {
        if (fovValueDisplay == null || fovValueDisplay.isDead())
            return;
        int fov = playerSettings.getOrDefault("fov", 90);
        fovValueDisplay.text(Component.text(String.valueOf(fov))
                .color(TextColor.fromHexString("#FFAA00"))
                .decorate(TextDecoration.BOLD));
    }

    private void sendCameraPacket(int entityId) {
        try {
            PacketContainer pkt = ProtocolLibrary.getProtocolManager()
                    .createPacket(PacketType.Play.Server.CAMERA);
            pkt.getIntegers().write(0, entityId);
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, pkt);
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to send CAMERA packet: " + t.getMessage());
        }
    }

    private static boolean hudReflectionLogged = false;

    private Object findConnection(Object handle) throws Exception {
        Class<?> cls = handle.getClass();
        while (cls != null && cls != Object.class) {
            for (java.lang.reflect.Field f : cls.getDeclaredFields()) {
                String typeName = f.getType().getName();
                if (typeName.contains("ServerGamePacketListenerImpl")
                        || typeName.contains("PlayerConnection")
                        || "connection".equals(f.getName())
                        || "c".equals(f.getName()) && typeName.contains("Listener")) {
                    f.setAccessible(true);
                    Object value = f.get(handle);
                    if (value != null)
                        return value;
                }
            }
            cls = cls.getSuperclass();
        }
        return null;
    }

    private Object findChangeGameModeReason(Class<?> typeClass) {
        try {
            java.lang.reflect.Field f = typeClass.getField("CHANGE_GAME_MODE");
            return f.get(null);
        } catch (Throwable ignored) {
        }
        try {
            for (java.lang.reflect.Field f : typeClass.getDeclaredFields()) {
                String n = f.getName().toUpperCase();
                if (n.contains("GAME_MODE") || n.contains("GAMEMODE")) {
                    f.setAccessible(true);
                    Object v = f.get(null);
                    if (v != null)
                        return v;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            java.lang.reflect.Constructor<?> ctor = typeClass.getDeclaredConstructor(int.class);
            ctor.setAccessible(true);
            return ctor.newInstance(3);
        } catch (Throwable ignored) {
        }
        try {
            java.lang.reflect.Field idMapField = typeClass.getDeclaredField("BY_ID");
            idMapField.setAccessible(true);
            Object map = idMapField.get(null);
            if (map instanceof Object[]) {
                Object[] arr = (Object[]) map;
                if (arr.length > 3)
                    return arr[3];
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void sendFakeGamemode(int gamemodeId) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object conn = findConnection(handle);
            if (conn == null) {
                if (!hudReflectionLogged) {
                    hudReflectionLogged = true;
                    plugin.getLogger()
                            .warning("[OrjusMenu] HUD: connection field not found on " + handle.getClass().getName());
                }
                return;
            }

            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundGameEventPacket");
            Class<?> typeClass = Class.forName("net.minecraft.network.protocol.game.ClientboundGameEventPacket$Type");
            Object reason = findChangeGameModeReason(typeClass);
            if (reason == null) {
                if (!hudReflectionLogged) {
                    hudReflectionLogged = true;
                    StringBuilder sb = new StringBuilder(
                            "[OrjusMenu] HUD: CHANGE_GAME_MODE field not found. Available static fields of ");
                    sb.append(typeClass.getName()).append(":");
                    for (java.lang.reflect.Field f : typeClass.getDeclaredFields()) {
                        if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                            sb.append(" ").append(f.getName()).append("(").append(f.getType().getSimpleName())
                                    .append(")");
                        }
                    }
                    plugin.getLogger().warning(sb.toString());
                }
                return;
            }

            Object packet = packetClass.getConstructor(typeClass, float.class)
                    .newInstance(reason, (float) gamemodeId);

            Class<?> packetBaseClass = Class.forName("net.minecraft.network.protocol.Packet");
            java.lang.reflect.Method sendMethod = null;
            Class<?> connCls = conn.getClass();
            while (connCls != null && sendMethod == null) {
                for (java.lang.reflect.Method m : connCls.getDeclaredMethods()) {
                    if ((m.getName().equals("send") || m.getName().equals("sendPacket"))
                            && m.getParameterCount() == 1
                            && packetBaseClass.isAssignableFrom(m.getParameterTypes()[0])) {
                        m.setAccessible(true);
                        sendMethod = m;
                        break;
                    }
                }
                connCls = connCls.getSuperclass();
            }
            if (sendMethod == null) {
                if (!hudReflectionLogged) {
                    hudReflectionLogged = true;
                    plugin.getLogger()
                            .warning("[OrjusMenu] HUD: send method not found on " + conn.getClass().getName());
                }
                return;
            }
            sendMethod.invoke(conn, packet);
        } catch (Throwable t) {
            if (!hudReflectionLogged) {
                hudReflectionLogged = true;
                plugin.getLogger()
                        .warning("[OrjusMenu] HUD-hide failed: " + t.getClass().getSimpleName() + " " + t.getMessage());
            }
        }
    }

    private void spawnCameraPig() {
        cameraEntity = (Pig) anchor.getWorld().spawnEntity(anchor, EntityType.PIG);
        cameraEntity.setAI(false);
        cameraEntity.setGravity(false);
        cameraEntity.setInvisible(true);
        cameraEntity.setCollidable(false);
        cameraEntity.setSilent(true);
        cameraEntity.setInvulnerable(true);
        cameraEntity.setPersistent(false);
        cameraEntity.setRotation(anchorYaw, anchorPitch);
    }

    private static final float CURSOR_Z_OFFSET = -0.40f;
    private static final float CURSOR_Y_OFFSET = -0.30f;

    private void spawnCursor() {
        Location loc = computeLocAt(0f, 0f, distance + CURSOR_Z_OFFSET);
        ConfigurationSection cfg = plugin.getConfig();

        ItemStack inActiveItem = resolveCustomItem(cfg.getString("cursor.in-active.ia-id", ""),
                cfg.getString("cursor.in-active.item.material", ""),
                cfg.getInt("cursor.in-active.item.custom-model-data", 0));
        ItemStack activeItem = resolveCustomItem(cfg.getString("cursor.active.ia-id", ""),
                cfg.getString("cursor.active.item.material", ""),
                cfg.getInt("cursor.active.item.custom-model-data", 0));
        if (inActiveItem != null && activeItem != null) {
            cursor = getCursor(loc, inActiveItem, activeItem);
        } else {
            String txt = cfg.getString("cursor.text", "✚");
            Component textComponent = Component.text(txt).color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD);
            cursor = getCursor(loc, textComponent);
        }
    }

    private Cursor getCursor(Location location, ItemStack inActiveItem, ItemStack activeItem) {
        ItemDisplay itemDisplay = location.getWorld().spawn(location, ItemDisplay.class, disp -> {
            disp.setBillboard(Display.Billboard.CENTER);
            disp.setPersistent(false);
            disp.setInvulnerable(true);
            disp.setItemStack(inActiveItem);
            disp.setViewRange(2f);
            // disp.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI);
            disp.setTeleportDuration(2);
            disp.setBrightness(new Display.Brightness(15, 15));

            Transformation t = disp.getTransformation();
            disp.setTransformation(new Transformation(
                    new Vector3f(cursorScale * 0.5f, -cursorScale * 0.5f, 0f),
                    t.getLeftRotation(),
                    new Vector3f(-cursorScale, cursorScale, cursorScale),
                    t.getRightRotation()));
        });
        return new ItemCursor(itemDisplay, inActiveItem, activeItem);
    }

    private Cursor getCursor(Location location, Component text) {
        TextDisplay textDisplay = location.getWorld().spawn(location, TextDisplay.class, td -> {
            td.setBillboard(Display.Billboard.CENTER);
            td.setSeeThrough(true);
            td.setShadowed(false);
            td.setPersistent(false);
            td.setInvulnerable(true);

            td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            td.setViewRange(2.0f);
            try {
                td.setTeleportDuration(2);
            } catch (Throwable ignored) {
            }
            Transformation t = td.getTransformation();
            td.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    t.getLeftRotation(),
                    new Vector3f(cursorScale, cursorScale, 1f),
                    t.getRightRotation()));
        });
        return null; // TODO: textCursor intilize
    }

    private ItemStack resolveCustomItem(String iaId, String material, int customModelData) {
        if (iaId != null && !iaId.isEmpty()) {
            ItemStack ia = tryItemsAdderItem(iaId);
            if (ia != null)
                return ia;
        }
        if (material != null && !material.isEmpty()) {
            try {
                Material mat = Material.valueOf(material.toUpperCase());
                ItemStack item = new ItemStack(mat);
                if (customModelData > 0) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setCustomModelData(customModelData);
                        item.setItemMeta(meta);
                    }
                }
                return item;
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[OrjusMenu] Unknown material '" + material + "'");
            }
        }
        return null;
    }

    private ItemStack tryItemsAdderItem(String iaId) {
        try {
            Class<?> stackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Object stack = stackClass.getMethod("getInstance", String.class).invoke(null, iaId);
            if (stack == null)
                return null;
            return (ItemStack) stackClass.getMethod("getItemStack").invoke(stack);
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("[OrjusMenu] cursor.ia-id set but ItemsAdder is not loaded");
            return null;
        } catch (Throwable t) {
            plugin.getLogger().warning("[OrjusMenu] ItemsAdder lookup failed for '" + iaId + "': " + t.getMessage());
            return null;
        }
    }

    public void handleMove(PlayerMoveEvent e) {
    }

    public void handleRawRotation(float yaw, float pitch) {
    }

    public void tick() {
        if (!player.isOnline())
            return;
        if (cursor == null || cursor.getDisplay().isDead())
            return;
        if (cameraEntity == null || cameraEntity.isDead())
            return;

        cameraEntity.setRotation(anchorYaw, anchorPitch);

        if (!cameraEntity.getPassengers().contains(player)) {
            cameraEntity.addPassenger(player);
            sendCameraPacket(cameraEntity.getEntityId());
        }

        debugCounter++;
        if ((debugCounter % 60) == 0) {
            sendCameraPacket(cameraEntity.getEntityId());
        }

        Location loc = player.getLocation();
        float rawYaw = loc.getYaw();
        float rawPitch = loc.getPitch();

        if (!rotInitialized) {
            logicalYaw = rawYaw;
            lastRawYaw = rawYaw;
            logicalPitch = rawPitch;
            lastRawPitch = rawPitch;
            rotInitialized = true;
        } else {
            float dYaw = rawYaw - lastRawYaw;
            if (dYaw > 180f)
                dYaw -= 360f;
            else if (dYaw < -180f)
                dYaw += 360f;
            logicalYaw += dYaw;
            lastRawYaw = rawYaw;

            float dPitch = rawPitch - lastRawPitch;
            logicalPitch += dPitch;
            lastRawPitch = rawPitch;
        }

        float deltaFromAnchor = logicalYaw - anchorYaw;
        if (deltaFromAnchor > yawDegPerEdge) {
            logicalYaw = anchorYaw + yawDegPerEdge;
            deltaFromAnchor = yawDegPerEdge;
        } else if (deltaFromAnchor < -yawDegPerEdge) {
            logicalYaw = anchorYaw - yawDegPerEdge;
            deltaFromAnchor = -yawDegPerEdge;
        }

        float pitchDelta = logicalPitch - anchorPitch;
        if (pitchDelta > pitchDegPerEdge) {
            logicalPitch = anchorPitch + pitchDegPerEdge;
            pitchDelta = pitchDegPerEdge;
        } else if (pitchDelta < -pitchDegPerEdge) {
            logicalPitch = anchorPitch - pitchDegPerEdge;
            pitchDelta = -pitchDegPerEdge;
        }

        float screenX = (deltaFromAnchor / yawDegPerEdge) * halfW;
        float screenY = -(pitchDelta / pitchDegPerEdge) * halfH;

        if (screenX > halfW)
            screenX = halfW;
        if (screenX < -halfW)
            screenX = -halfW;
        if (screenY > halfH)
            screenY = halfH;
        if (screenY < -halfH)
            screenY = -halfH;

        float visualX = screenX;
        float visualY = screenY + CURSOR_Y_OFFSET;
        cursor.teleport(computeLocAt(visualX, visualY, distance + CURSOR_Z_OFFSET));

        float distanceRatio = (distance + CURSOR_Z_OFFSET) / distance;
        updateHover(visualX / distanceRatio, visualY / distanceRatio);
    }

    public void reattachCamera() {
        if (cameraEntity != null && !cameraEntity.isDead()) {
            sendCameraPacket(cameraEntity.getEntityId());
            if (!cameraEntity.getPassengers().contains(player)) {
                cameraEntity.addPassenger(player);
            }
        }
    }

    @SuppressWarnings({ "rawtypes" })
    private void spawnButtons() {
        List<?> list = plugin.getConfig().getList("buttons");
        if (list == null)
            return;
        float s = fovScale();
        for (Object o : list) {
            if (!(o instanceof Map))
                continue;
            Map map = (Map) o;
            String id = stringOf(map.get("id"), "btn");
            String text = stringOf(map.get("text"), id);
            float sx = numberOf(map.get("sx"), 0f) * s;
            float sy = numberOf(map.get("sy"), 0f) * s;
            float hw = numberOf(map.get("half-width"), 0.18f) * s;
            float hh = numberOf(map.get("half-height"), 0.08f) * s;
            float scale = numberOf(map.get("scale"), 1.0f);
            String idleColorHex = stringOf(map.get("color"), "#FFFFFF");
            String hoverColorHex = stringOf(map.get("hover-color"), "#FFAA00");
            String cmd = stringOf(map.get("command"), "");

            TextColor idleColor = parseColor(idleColorHex, NamedTextColor.WHITE);
            TextColor hoverColor = parseColor(hoverColorHex, NamedTextColor.GOLD);

            MenuButton b = new MenuButton(id, text, sx, sy, hw, hh, scale, idleColor, hoverColor, cmd);
            Location loc = computeCursorLoc(sx, sy);
            b.spawn(loc);
            buttons.add(b);
        }
    }

    private static String stringOf(Object o, String def) {
        return o == null ? def : String.valueOf(o);
    }

    private static float numberOf(Object o, float def) {
        if (o instanceof Number n)
            return n.floatValue();
        if (o instanceof String s) {
            try {
                return Float.parseFloat(s);
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        return def;
    }

    private static TextColor parseColor(String hex, TextColor fallback) {
        try {
            if (hex == null)
                return fallback;
            if (hex.startsWith("#"))
                return TextColor.fromHexString(hex);
            return fallback;
        } catch (Throwable t) {
            return fallback;
        }
    }

    private void updateHover(float sx, float sy) {
        MenuButton newHover = null;
        for (MenuButton b : buttons) {
            if (b.contains(sx, sy)) {
                newHover = b;
                break;
            }
        }
        if (newHover != currentHover) {
            if (currentHover != null)
                currentHover.setHovered(false);
            if (newHover != null) {
                newHover.setHovered(true);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.8f);
                cursor.setActive();
            } else {
                cursor.setInActive();
            }
            currentHover = newHover;
        }
    }

    public void handleClick() {
        if (currentHover == null)
            return;
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        String action = currentHover.command;
        if (action == null || action.isEmpty())
            return;

        if (action.startsWith("open:")) {
            String target = action.substring(5).trim();
            loadScreen(target);
            return;
        }

        if (action.startsWith("change:")) {
            String[] parts = action.substring(7).split(":");
            if (parts.length >= 4) {
                String key = parts[0];
                int delta = (int) Float.parseFloat(parts[1]);
                int min = (int) Float.parseFloat(parts[2]);
                int max = (int) Float.parseFloat(parts[3]);
                int current = playerSettings.getOrDefault(key, 0);
                int newVal = Math.max(min, Math.min(max, current + delta));
                playerSettings.put(key, newVal);
                plugin.setPlayerSetting(player.getUniqueId(), key, newVal);
                if (key.equals("fov")) {
                    applyFovScale();
                    if ("settings".equals(currentScreen)) {
                        loadScreen("settings");
                    }
                }
            }
            return;
        }

        String cmd = action;
        if (action.startsWith("command:"))
            cmd = action.substring(8).trim();
        final String finalCmd = cmd.replace("%player%", player.getName());
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd));
    }

    public String getCurrentScreen() {
        return currentScreen;
    }

    private Location computeCursorLoc(float screenX, float screenY) {
        return computeLocAt(screenX, screenY, distance);
    }

    private Location computeLocAt(float screenX, float screenY, float dist) {
        Location base = (cameraEntity != null && !cameraEntity.isDead())
                ? cameraEntity.getEyeLocation()
                : anchor.clone().add(0, 0.62, 0);

        double yawRad = Math.toRadians(anchorYaw);
        double pitchRad = Math.toRadians(anchorPitch);

        Vector dir = new Vector(
                -Math.sin(yawRad) * Math.cos(pitchRad),
                -Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)).normalize();

        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        Vector up = right.clone().getCrossProduct(dir).normalize();

        double x = base.getX() + dir.getX() * dist + right.getX() * screenX + up.getX() * screenY;
        double y = base.getY() + dir.getY() * dist + right.getY() * screenX + up.getY() * screenY;
        double z = base.getZ() + dir.getZ() * dist + right.getZ() * screenX + up.getZ() * screenY;

        return new Location(base.getWorld(), x, y, z, anchorYaw, anchorPitch);
    }
}
