package pl.owntelecom.managers;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import pl.owntelecom.OwnTelecom;

public class ConfigManager {

    private final OwnTelecom plugin;
    private FileConfiguration config;

    public ConfigManager(OwnTelecom plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public String getMessage(String path) {
        return ChatColor.translateAlternateColorCodes('&', 
            config.getString(path, "&cBrak komunikatu: " + path));
    }

    // Chat
    public boolean isLocalChatEnabled() { return config.getBoolean("chat.lokalny.enabled", true); }
    public int getChatRadius() { return config.getInt("chat.lokalny.promien", 10); }
    public String getNoListenerMessage() { return getMessage("chat.lokalny.brak_odbiorcy_komunikat"); }
    public boolean isGlobalChatEnabled() { return config.getBoolean("chat.globalny.enabled", false); }

    // Operator
    public int getOperatorCooldownDays() { return config.getInt("operator.cooldown_dni", 7); }
    public int getMaxOperatorsPerPlayer() { return config.getInt("operator.max_operatorow_na_gracza", 1); }

    // Polaczenia
    public int getMessagesPerMinute() { return config.getInt("polaczenia.wiadomosci_na_minute", 1); }
    public boolean isDistortionEnabled() { return config.getBoolean("polaczenia.znieksztalcanie_przy_slabym_zasiegu", true); }

    // Stacje
    public Material getStationBlock() { 
        return Material.getMaterial(config.getString("stacje.budowa.blok", "IRON_BLOCK")); 
    }
    public Material getStationAddition() { 
        return Material.getMaterial(config.getString("stacje.budowa.dodatek", "OAK_FENCE")); 
    }
    public double getBaseFailureChance() { return config.getDouble("stacje.awarie.szansa_bazowa", 10.0); }
    public double getRepairCost() { return config.getDouble("stacje.awarie.koszt_naprawy", 1000.0); }

    // Vault
    public String getVaultPrefix() { return config.getString("vault.prefix_konta", ""); }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }
}
