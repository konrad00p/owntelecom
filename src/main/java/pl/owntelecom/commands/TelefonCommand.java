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
                    player.sendMessage(ChatColor.RED + "  ⚠ SMS wyczerpane!");
                }
                if (sub.getRemainingMegabytes() != -1 && sub.getRemainingMegabytes() == 0) {
                    player.sendMessage(ChatColor.RED + "  ⚠ Dane wyczerpane!");
                }
                
                player.sendMessage("");
            }
        }
    }

    private void handleRates(Player player, String[] args) {
        String type = args[1].toLowerCase();
        
        if (type.equals("krajowy") || type.equals("domestic")) {
            player.sendMessage(ChatColor.GOLD + "══════ Stawki krajowe ══════");
            var operators = plugin.getOperatorManager().getActiveOperators();
            
            for (var op : operators) {
                if (plugin.getStationManager().isPlayerInRange(player, op.getId())) {
                    player.sendMessage(ChatColor.YELLOW + "▸ " + op.getDisplayName());
                    player.sendMessage(ChatColor.GRAY + "  Minuta: $" + op.getRate("minuta"));
                    player.sendMessage(ChatColor.GRAY + "  SMS: $" + op.getRate("sms"));
                    player.sendMessage(ChatColor.GRAY + "  MB: $" + op.getRate("mb"));
                }
            }
        } else if (type.equals("roaming")) {
            player.sendMessage(ChatColor.GOLD + "══════ Stawki roamingowe ══════");
            var operators = plugin.getOperatorManager().getActiveOperators();
            
            for (var op : operators) {
                if (plugin.getAgreementManager().isPlayerRoaming(player, op.getId())) {
                    player.sendMessage(ChatColor.YELLOW + "▸ " + op.getDisplayName() + " (Roaming)");
                    player.sendMessage(ChatColor.GRAY + "  Minuta: $" + op.getRoamingRate("minuta"));
                    player.sendMessage(ChatColor.GRAY + "  SMS: $" + op.getRoamingRate("sms"));
                    player.sendMessage(ChatColor.GRAY + "  MB: $" + op.getRoamingRate("mb"));
                }
            }
        }
    }

    private void handleOperatorInfo(Player player) {
        player.sendMessage(ChatColor.GOLD + "══════ Dostępni operatorzy ══════");
        
        for (var op : plugin.getOperatorManager().getActiveOperators()) {
            boolean inRange = plugin.getStationManager().isPlayerInRange(player, op.getId());
            String status = inRange ? ChatColor.GREEN + "✓ W zasięgu" : ChatColor.RED + "✗ Brak zasięgu";
            
            player.sendMessage(ChatColor.YELLOW + "▸ " + op.getDisplayName() + " " + status);
            
            var bestStation = plugin.getStationManager().findBestStation(player, op.getId());
            if (bestStation != null) {
                player.sendMessage(ChatColor.GRAY + "  Technologia: " + bestStation.getTechnology() + 
                    " | Jakość: " + String.format("%.0f%%", bestStation.getSignalQuality(player.getLocation()) * 100));
            }
        }
    }

    private void showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "══════ 📱 Telefon - Pomoc ══════");
        player.sendMessage(ChatColor.YELLOW + "/telefon info " + ChatColor.WHITE + "- Stan konta i pakietów");
        player.sendMessage(ChatColor.YELLOW + "/telefon pakiety " + ChatColor.WHITE + "- Dostępne oferty");
        player.sendMessage(ChatColor.YELLOW + "/telefon kup <ID> " + ChatColor.WHITE + "- Kup pakiet");
        player.sendMessage(ChatColor.YELLOW + "/telefon pakiet " + ChatColor.WHITE + "- Status pakietu");
        player.sendMessage(ChatColor.YELLOW + "/telefon stawki <krajowy/roaming> " + ChatColor.WHITE + "- Stawki");
        player.sendMessage(ChatColor.YELLOW + "/telefon operator " + ChatColor.WHITE + "- Dostępni operatorzy");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("info", "pakiety", "kup", "pakiet", "stawki", "operator"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "kup":
                    completions.addAll(plugin.getBillingManager().getAllSubscriptions().stream()
                        .map(Subscription::getId)
                        .collect(Collectors.toList()));
                    break;
                case "stawki":
                    completions.addAll(Arrays.asList("krajowy", "roaming"));
                    break;
            }
        }

        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
}
