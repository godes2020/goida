package ru.orjus.menu.config;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class Config {
    private final File file;
    private static JavaPlugin plugin;
    private YamlConfiguration config;

    public static void load(JavaPlugin plugin) {
        Config.plugin = plugin;
    }

    public Config(String fileName) {
        File file = new File(plugin.getDataFolder().getAbsolutePath() + "/" + fileName + ".yml");
        this.file = file;

        checkFileExists();
    }

    public Config(File file) {
        this.file = file;
        checkFileExists();
    }

    private void checkFileExists() {
        if (!file.exists())
            createFileFromResources();
        if (file.exists())
            load();
    }

    protected void createFileFromResources() {
        if (hasResource())
            plugin.saveResource(getFilePath(), true);
    }

    private void createThisFile() {
        try {
            file.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean hasResource() {
        return plugin.getResource(getFilePath()) != null;
    }

    public String getFilePath() {
        return file.getAbsolutePath().substring(plugin.getDataFolder().getAbsolutePath().length() + 1);
    }

    public boolean isValid() {
        return hasResource() || file.exists();
    }

    protected void load() {
        config = YamlConfiguration.loadConfiguration(file);
        config.options().parseComments(true);
        try {
            config.load(file);
        } catch (Exception exception) {
            Bukkit.getLogger().severe(plugin.getName() + " Failed to load file: " + file.getPath());
        }

    }

    public void save() {
        try {
            config.save(file);
        } catch (Exception exception) {
            Bukkit.getLogger().severe("AntGame: Failed to save file: " + file.getPath());
            exception.printStackTrace();
        }
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public File getFile() {
        return file;
    }
}