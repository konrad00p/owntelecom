package pl.owntelecom.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.owntelecom.OwnTelecom;
import pl.owntelecom.models.Subscription;

import java.util.*;
import java.util.stream.Collectors;

public class TelefonCommand implements CommandExecutor, TabCompleter {

    private final OwnTelecom plugin;

    public TelefonCommand(OwnTelecom plugin) {
        this.plugin = plugin;
        plugin.getCommand("telefon").setExecutor(this);
        plugin.getCommand("telefon").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Ta komenda jest tylko dla graczy!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info":
            case "konto":
                plugin.getBillingManager().showAccountInfo(player);
                break;

            case "pakiety":
            case "oferta":
                plugin.getBillingManager().showAvailablePackages(player);
                break;

            case "kup":
            case "buy":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użycie: /telefon kup <ID_Pakietu>");
                    return true;
                }
                plugin.getBillingManager().purchaseSubscription(player, args[1]);
                break;

            case "pakiet":
            case "status":
                handlePackageStatus(player);
                break;

            case "stawki":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użycie: /telefon stawki <roaming/krajowy> [operator]");
                    return true;
                }
                handleRates(player, args);
                break;

            case "operator":
                handleOperatorInfo(player);
                break;

            default:
                showHelp(player);
                break;
        }

        return true;
    }

    private void handlePackageStatus(Player player) {
        var activeSubs = plugin.getBillingManager().getPlayerActiveSubscriptions(player.getUniqueId());
        
        player.sendMessage(ChatColor.GOLD + "══════ 📦 Status pakietów ══════");
        
        if (activeSubs.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Nie masz aktywnych pakietów.");
            player.sendMessage(ChatColor.GRAY + "Korzystasz ze stawki PREPAID (na kartę).");
            player.sendMessage(ChatColor.YELLOW + "/telefon pakiety - zobacz dostępne oferty");
        } else {
            for (var sub : activeSubs) {
                Subscription original = plugin.getBillingManager().getSubscription(sub.getSubscriptionId());
                String name = original != null ? original.getName() : "Nieznany";
                
                player.sendMessage(ChatColor.YELLOW + "▸ " + name);
                player.sendMessage(sub.getStatusText());
                
                if (sub.getRemainingMinutes() != -1 && sub.getRemainingMinutes() == 0) {
                    player.sendMessage(ChatColor.RED + "  ⚠ Minuty wyczerpane!");
                }
                if (sub.getRemainingSms() != -1 && sub.getRemainingSms() == 0) {
