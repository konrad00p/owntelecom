package pl.owntelecom;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import pl.owntelecom.commands.*;
import pl.owntelecom.listeners.ChatListener;
import pl.owntelecom.listeners.CallListener;
import pl.owntelecom.managers.*;

public final class OwnTelecom extends JavaPlugin {

    private static OwnTelecom instance;
    private Economy economy;
    
    // Managery
    private ConfigManager configManager;
    private OperatorManager operatorManager;
    private StationManager stationManager;
    private CallManager callManager;
    private InternetManager internetManager;
    
    // Listenery
    private ChatListener chatListener;

    @Override
    public void onEnable() {
        instance = this;
        
        // Inicjalizacja Vault
        if (!setupEconomy()) {
            getLogger().severe("Vault nie został znaleziony! Plugin zostanie wyłączony.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Zapisz domyślne pliki konfiguracyjne
        saveDefaultConfig();
        
        // Inicjalizacja managerów
        this.configManager = new ConfigManager(this);
        this.operatorManager = new OperatorManager(this);
        this.stationManager = new StationManager(this);
        this.callManager = new CallManager(this);
        this.internetManager = new InternetManager(this);
        
        // Rejestracja listenerów (przed komendami)
        registerListeners();
        
        // Rejestracja komend
        registerCommands();
        
        getLogger().info("OwnTelecom został pomyślnie włączony!");
    }

    @Override
    public void onDisable() {
        // Zapisanie danych przed wyłączeniem
        if (operatorManager != null) operatorManager.saveAll();
        if (stationManager != null) stationManager.saveAll();
        
        // Wyczyść listenery
        if (chatListener != null) {
            chatListener = null;
        }
        
        getLogger().info("OwnTelecom został wyłączony.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void registerCommands() {
        // Komendy podstawowe
        new TelefonCommand(this);
        new CallCommand(this);
        new SmsCommand(this);
        new OdbierzCommand(this);
        new RozlaczCommand(this);
        new AlarmowyCommand(this);
        new OperatorCommand(this);
        new StacjaCommand(this);
        new InternetCommand(this);
        
        // Komendy administracyjne
        new ChatCommand(this, chatListener);
    }

    private void registerListeners() {
        // ChatListener
        this.chatListener = new ChatListener(this);
        getServer().getPluginManager().registerEvents(chatListener, this);
        
        // CallListener - NOWY! Obsługa połączeń i rozłączania graczy
        getServer().getPluginManager().registerEvents(new CallListener(this), this);
    }

    // Gettery
    public static OwnTelecom getInstance() { return instance; }
    public Economy getEconomy() { return economy; }
    public ConfigManager getConfigManager() { return configManager; }
    public OperatorManager getOperatorManager() { return operatorManager; }
    public StationManager getStationManager() { return stationManager; }
    public CallManager getCallManager() { return callManager; }
    public InternetManager getInternetManager() { return internetManager; }
    public ChatListener getChatListener() { return chatListener; }
}
