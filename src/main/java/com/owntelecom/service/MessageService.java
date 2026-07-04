package com.owntelecom.service;

import com.owntelecom.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;

public class MessageService {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final ConfigManager configManager;

    public MessageService(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, Map.of());
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        String prefix = configManager.getMessages().getString("prefix", "");
        String raw = configManager.getMessages().getString(path, path);
        Map<String, String> all = new HashMap<>(placeholders);
        all.put("prefix", prefix);
        all.put("currency", configManager.getConfig().getString("economy.currency-symbol", "$"));
        for (Map.Entry<String, String> e : all.entrySet()) {
            raw = raw.replace("{" + e.getKey() + "}", e.getValue());
        }
        sender.sendMessage(colorize(raw));
    }

    public Component colorize(String text) {
        return LEGACY.deserialize(text);
    }

    public String getRaw(String path) {
        return configManager.getMessages().getString(path, path);
    }
}
