package ru.orjus.menu;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public final class MenuPlugin extends JavaPlugin {

    private static MenuPlugin instance;
    private MenuManager menuManager;

    private File playerDataFile;
    private YamlConfiguration playerData;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        loadPlayerData();
        installItemsAdderResources();

        this.menuManager = new MenuManager(this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(menuManager), this);

        try {
            new RotationPacketListener(this, menuManager).register();
        } catch (Throwable t) {
            getLogger().warning("ProtocolLib hook failed: " + t.getMessage());
        }

        Bukkit.getScheduler().runTaskTimer(this, menuManager::tickAll, 1L, 1L);

        for (Player p : Bukkit.getOnlinePlayers()) {
            menuManager.openMenu(p);
        }
        getLogger().info("OrjusMenu enabled");
    }

    @Override
    public void onDisable() {
        if (menuManager != null) menuManager.shutdown();
        savePlayerData();
    }

    private void loadPlayerData() {
        getDataFolder().mkdirs();
        playerDataFile = new File(getDataFolder(), "playerdata.yml");
        if (!playerDataFile.exists()) {
            try { playerDataFile.createNewFile(); } catch (IOException ignored) {}
        }
        playerData = YamlConfiguration.loadConfiguration(playerDataFile);
    }

    public void savePlayerData() {
        if (playerData == null || playerDataFile == null) return;
        try {
            playerData.save(playerDataFile);
        } catch (IOException e) {
            getLogger().warning("Could not save playerdata.yml: " + e.getMessage());
        }
    }

    public int getPlayerSetting(UUID uuid, String key, int def) {
        if (playerData == null) return def;
        return playerData.getInt(uuid + "." + key, def);
    }

    private void installItemsAdderResources() {
        File iaPlugin = new File("plugins/ItemsAdder");
        if (!iaPlugin.exists()) {
            getLogger().info("ItemsAdder not detected — skipping resource pack install (plugin still works in text-cursor mode)");
            return;
        }

        File contents = new File(iaPlugin, "contents");
        contents.mkdirs();
        File targetBase = new File(contents, "orjusmenu");

        String[] files = new String[] {
                "configs/cursor.yml",
                "resourcepack/assets/orjusmenu/textures/item/cursor.png",
                "resourcepack/assets/orjusmenu/models/item/cursor.json"
        };

        int copied = 0;
        for (String relPath : files) {
            File target = new File(targetBase, relPath);
            if (target.exists()) continue;
            target.getParentFile().mkdirs();
            try (InputStream in = getResource("itemsadder/" + relPath)) {
                if (in == null) {
                    getLogger().warning("Bundled resource missing: itemsadder/" + relPath);
                    continue;
                }
                Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                copied++;
            } catch (IOException e) {
                getLogger().warning("Failed to copy " + relPath + ": " + e.getMessage());
            }
        }

        if (copied > 0) {
            getLogger().info("Installed " + copied + " ItemsAdder resource files into plugins/ItemsAdder/contents/orjusmenu/");
            getLogger().info("Run /iazip and /iareload (or restart) to apply the resource pack on clients.");
        }
    }

    public void setPlayerSetting(UUID uuid, String key, int value) {
        if (playerData == null) return;
        playerData.set(uuid + "." + key, value);
        savePlayerData();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("toggle")) {
            if (menuManager.hasSession(p)) menuManager.closeMenu(p);
            else menuManager.openMenu(p);
            return true;
        }
        if (args[0].equalsIgnoreCase("open")) {
            menuManager.openMenu(p);
        } else if (args[0].equalsIgnoreCase("close")) {
            menuManager.closeMenu(p);
        }
        return true;
    }

    public static MenuPlugin get() { return instance; }
    public MenuManager getMenuManager() { return menuManager; }
}
