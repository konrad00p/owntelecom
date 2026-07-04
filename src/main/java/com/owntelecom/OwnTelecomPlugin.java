package com.owntelecom;

import com.owntelecom.command.*;
import com.owntelecom.config.ConfigManager;
import com.owntelecom.database.DatabaseManager;
import com.owntelecom.module.ModuleManager;
import com.owntelecom.service.EconomyService;
import com.owntelecom.service.MessageService;
import org.bukkit.plugin.java.JavaPlugin;

public final class OwnTelecomPlugin extends JavaPlugin {

    private static OwnTelecomPlugin instance;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private MessageService messageService;
    private EconomyService economyService;
    private ModuleManager moduleManager;

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        configManager.loadAll();

        messageService = new MessageService(configManager);
        databaseManager = new DatabaseManager(this, configManager);
        databaseManager.init();

        economyService = new EconomyService(this);
        moduleManager = new ModuleManager(this);
        moduleManager.enableAll();

        registerCommands();

        getLogger().info("OwnTelecom wlaczony.");
    }

    @Override
    public void onDisable() {
        if (moduleManager != null) {
            moduleManager.disableAll();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        instance = null;
        getLogger().info("OwnTelecom wylaczony.");
    }

    private void registerCommands() {
        new OwnTelecomCommand(this).register();
        new OperatorCommand(this).register();
        new StationCommand(this).register();
        new PhoneCommand(this).register();
        new CallCommand(this).register();
        new SmsCommand(this).register();
        new EmergencyCommand(this).register();
        new InternetCommand(this).register();
        new AgreementCommand(this).register();
        new PackageCommand(this).register();
        new WebsiteCommand(this).register();
        new StawkiCommand(this).register();
    }

    public static OwnTelecomPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public EconomyService getEconomyService() {
        return economyService;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }
}
