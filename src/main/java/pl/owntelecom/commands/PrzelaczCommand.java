package pl.owntelecom.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.owntelecom.OwnTelecom;
import pl.owntelecom.models.Operator;

import java.util.*;
import java.util.stream.Collectors;

public class PrzelaczCommand implements CommandExecutor, TabCompleter {

    private final OwnTelecom plugin;

    public PrzelaczCommand(OwnTelecom plugin) {
        this.plugin = plugin;
        plugin.getCommand("przelacz").setExecutor(this);
        plugin.getCommand("przelacz").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Ta komenda jest tylko dla graczy!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Użycie: /przelacz <operator>");
            player.sendMessage(ChatColor.GRAY + "/przelacz auto - automatyczny wybór");
            player.sendMessage(ChatColor.GRAY + "/przelacz home - powrót do głównego");
            return true;
        }

        String target = args[0].toLowerCase();

        if (target.equals("auto")) {
            String currentOp = plugin.getNetworkManager().getConnectedOperator(player);
            if (currentOp != null) {
                // Znajdź najlepszy roaming
                player.sendMessage(ChatColor.YELLOW + "🔍 Szukam najlepszej sieci...");
                
                Operator bestOp = null;
                double bestQuality = -1;
                
                for (Operator op : plugin.getOperatorManager().getActiveOperators()) {
                    var station = plugin.getStationManager().findBestStation(player, op.getId());
                    if (station != null) {
                        double quality = station.getSignalQuality(player.getLocation());
                        if (quality > bestQuality) {
                            bestQuality = quality;
                            bestOp = op;
                        }
                    }
                }
                
                if (bestOp != null && !bestOp.getId().equals(currentOp)) {
                    plugin.getNetworkManager().switchOperator(player, bestOp.getId());
                } else {
                    player.sendMessage(ChatColor.GREEN + "✅ Już jesteś w najlepszej dostępnej sieci.");
                }
            }
        } else if (target.equals("home")) {
            // Wróć do własnego operatora
            var playerOps = plugin.getOperatorManager().getOperatorsByOwner(player.getUniqueId());
            if (!playerOps.isEmpty()) {
                String homeOp = playerOps.get(0).getId();
                if (plugin.getStationManager().isPlayerInRange(player, homeOp)) {
                    plugin.getNetworkManager().switchOperator(player, homeOp);
                } else {
                    player.sendMessage(ChatColor.RED + "Twój główny operator nie ma zasięgu w tym miejscu!");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Nie posiadasz własnego operatora!");
            }
        } else if (target.equals("potwierdz")) {
            // Potwierdzenie wymuszonego przełączenia (roaming awaryjny)
            player.sendMessage(ChatColor.RED + "Użyj /przelacz <operator> potwierdz");
        } else if (args.length >= 2 && args[1].equalsIgnoreCase("potwierdz")) {
            // Wymuś przełączenie
            plugin.getNetworkManager().forceSwitchOperator(player, args[0]);
        } else {
            // Normalne przełączenie
            plugin.getNetworkManager().switchOperator(player, args[0]);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("auto");
            completions.add("home");
            
            if (sender instanceof Player) {
                Player player = (Player) sender;
                completions.addAll(plugin.getOperatorManager().getActiveOperators().stream()
                    .filter(op -> plugin.getStationManager().isPlayerInRange(player, op.getId()))
                    .map(Operator::getId)
                    .collect(Collectors.toList()));
            }
            
            return completions.stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
