package com.owntelecom.config;

import com.owntelecom.OwnTelecomPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class ConfigManager {

    private final OwnTelecomPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration technologies;
    private FileConfiguration stations;
    private FileConfiguration internet;

    public ConfigManager(OwnTelecomPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        saveDefault("config.yml");
        saveDefault("messages.yml");
        saveDefault("technologies.yml");
        saveDefault("stations.yml");
        saveDefault("internet.yml");

        config = load("config.yml");
        messages = load("messages.yml");
        technologies = load("technologies.yml");
        stations = load("stations.yml");
        internet = load("internet.yml");
    }

    public void reloadAll() {
        loadAll();
    }

    private void saveDefault(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
    }

    private FileConfiguration load(String name) {
        File file = new File(plugin.getDataFolder(), name);
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        InputStream stream = plugin.getResource(name);
        if (stream != null) {
            yaml.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)));
        }
        return yaml;
    }

    public void saveInternet() {
        saveFile(internet, "internet.yml");
    }

    private void saveFile(FileConfiguration yaml, String name) {
        try {
            yaml.save(new File(plugin.getDataFolder(), name));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie mozna zapisac " + name, e);
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public FileConfiguration getTechnologies() {
        return technologies;
    }

    public FileConfiguration getStations() {
        return stations;
    }

    public FileConfiguration getInternet() {
        return internet;
    }

    public File getDataFile(String path) {
        return new File(plugin.getDataFolder(), path);
    }
}
