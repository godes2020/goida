package ru.orjus.menu.config;

public class MenuConfig {
    private static final Config config = new Config("config");
    private static MenuConfig instance = new MenuConfig();

    public void load() {
        // TODO: loading menus from config
    }

    public void reload() {
        instance = new MenuConfig();
        instance.load();
    }

    public static Config getConfig() {
        return config;
    }

    public static MenuConfig getInstance() {
        return instance;
    }
}
