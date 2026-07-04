package com.owntelecom.service;

import com.owntelecom.OwnTelecomPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Optional;

public class EconomyService {

    private final OwnTelecomPlugin plugin;
    private Economy economy;

    public EconomyService(OwnTelecomPlugin plugin) {
        this.plugin = plugin;
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            var rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
            }
        }
        if (economy == null) {
            plugin.getLogger().warning("Vault Economy nie znaleziony - platnosci wylaczone.");
        }
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public boolean has(Player player, double amount) {
        return economy != null && economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (economy == null || amount <= 0) {
            return economy == null && amount <= 0;
        }
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (economy == null || amount <= 0) {
            return false;
        }
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public double getBalance(Player player) {
        return economy != null ? economy.getBalance(player) : 0;
    }

    public Optional<Economy> getEconomy() {
        return Optional.ofNullable(economy);
    }
}
