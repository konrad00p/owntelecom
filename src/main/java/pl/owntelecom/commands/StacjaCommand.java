package pl.owntelecom.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.owntelecom.OwnTelecom;
import pl.owntelecom.models.Station;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StacjaCommand implements CommandExecutor, TabCompleter {

    private final OwnTelecom plugin;

    public StacjaCommand(OwnTelecom plugin) {
        this.plugin = plugin;
        plugin.getCommand("stacja").setExecutor(this);
        plugin.getCommand("stacja").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Ta komenda jest tylko dla graczy!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "utworz":
            case "create":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Użycie: /stacja utworz <ID_Operatora> <Technologia>");
                    player.sendMessage(ChatColor.GRAY + "Dostępne technologie: 2G, LTE, 5G");
                    return true;
                }
                handleCreate(player, args[1], args[2].toUpperCase());
                break;

            case "ulepsz":
            case "upgrade":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użycie: /stacja ulepsz <ID_Stacji>");
                    return true;
                }
                handleUpgrade(player, args[1]);
                break;

            case "napraw":
            case "repair":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użycie: /stacja napraw <ID_Stacji>");
                    return true;
                }
                handleRepair(player, args[1]);
                break;

            case "usun":
            case "delete":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użycie: /stacja usun <ID_Stacji>");
                    return true;
                }
                handleDelete(player, args[1]);
                break;

            case "info":
                if (args.length < 2) {
                    handleListMyStations(player);
                } else {
                    handleInfo(player, args[1]);
                }
                break;

            case "lista":
            case "list":
                handleList(player);
                break;

            case "zasieg":
            case "range":
                handleRangeCheck(player);
                break;

            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void handleCreate(Player player, String operatorId, String technology) {
        plugin.getStationManager().createStation(player, operatorId, technology);
    }

    private void handleUpgrade(Player player, String stationId) {
        plugin.getStationManager().upgradeStation(player, stationId);
    }

    private void handleRepair(Player player, String stationId) {
        plugin.getStationManager().repairStation(player, stationId);
    }

    private void handleDelete(Player player, String stationId) {
        plugin.getStationManager().removeStation(player, stationId);
    }

    private void handleInfo(Player player, String stationId) {
        Station station = plugin.getStationManager().getStation(stationId);
        if (station == null) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono stacji: " + stationId);
            return;
        }
        showStationInfo(player, station);
    }

    private void handleListMyStations(Player player) {
        List<Station> allStations = new ArrayList<>();
        plugin.getOperatorManager().getOperatorsByOwner(player.getUniqueId()).forEach(op -> {
            allStations.addAll(plugin.getStationManager().getOperatorStations(op.getId()));
        });

        if (allStations.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Nie posiadasz żadnych stacji.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== Twoje stacje ===");
        for (Station station : allStations) {
            String status = station.isBroken() ? ChatColor.RED + "⚠ AWARIA" : 
                           station.isActive() ? ChatColor.GREEN + "✓ Aktywna" : ChatColor.RED + "✗ Nieaktywna";
            player.sendMessage(ChatColor.YELLOW + "• " + station.getId() + 
                ChatColor.GRAY + " [" + station.getTechnology() + " Poziom " + station.getLevel() + "] " + status);
        }
    }

    private void handleList(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Wszystkie stacje ===");
        plugin.getStationManager().getAllStations().values().forEach(station -> {
            player.sendMessage(ChatColor.YELLOW + "• " + station.getId() + 
                ChatColor.GRAY + " - Operator: " + station.getOperatorId() + 
                " [" + station.getTechnology() + " Lvl." + station.getLevel() + "]");
        });
    }

    private void handleRangeCheck(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Sprawdzanie zasięgu ===");
        
        plugin.getOperatorManager().getAllOperators().forEach(op -> {
            Station bestStation = plugin.getStationManager().findBestStation(player, op.getId());
            if (bestStation != null) {
                double quality = bestStation.getSignalQuality(player.getLocation()) * 100;
                player.sendMessage(ChatColor.YELLOW + op.getDisplayName() + ": " + 
                    String.format("%.1f%%", quality) + " jakości | " + bestStation.getTechnology());
            } else {
                player.sendMessage(ChatColor.RED + op.getDisplayName() + ": Brak zasięgu");
            }
        });
    }

    private void showStationInfo(Player player, Station station) {
        player.sendMessage(ChatColor.GOLD + "=== Stacja: " + station.getId() + " ===");
        player.sendMessage(ChatColor.YELLOW + "Operator: " + ChatColor.WHITE + station.getOperatorId());
        player.sendMessage(ChatColor.YELLOW + "Technologia: " + ChatColor.WHITE + station.getTechnology());
        player.sendMessage(ChatColor.YELLOW + "Poziom: " + ChatColor.WHITE + station.getLevel() + "/3");
        player.sendMessage(ChatColor.YELLOW + "Zasięg: " + ChatColor.WHITE + station.getBaseRange() + " bloków");
        player.sendMessage(ChatColor.YELLOW + "Prędkość: " + ChatColor.WHITE + station.getBaseSpeed() + " Mb/s");
        player.sendMessage(ChatColor.YELLOW + "Status: " + (station.isActive() ? 
            ChatColor.GREEN + "Aktywna" : ChatColor.RED + "Nieaktywna"));
        player.sendMessage(ChatColor.YELLOW + "Awaria: " + (station.isBroken() ? 
            ChatColor.RED + "TAK - Koszt naprawy: $" + station.getRepairCost() : ChatColor.GREEN + "Nie"));
        player.sendMessage(ChatColor.YELLOW + "Szansa awarii: " + ChatColor.WHITE + station.getDamageChance() + "%");
        
        if (station.getLevel() < 3) {
            player.sendMessage(ChatColor.YELLOW + "Koszt ulepszenia: " + ChatColor.WHITE + "$" + station.getUpgradeCost());
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Stacje - Pomoc ===");
        player.sendMessage(ChatColor.YELLOW + "/stacja utworz <ID_Operatora> <2G/LTE/5G> " + ChatColor.WHITE + "- Tworzy stację");
        player.sendMessage(ChatColor.YELLOW + "/stacja ulepsz <ID_Stacji> " + ChatColor.WHITE + "- Ulepsza stację");
        player.sendMessage(ChatColor.YELLOW + "/stacja napraw <ID_Stacji> " + ChatColor.WHITE + "- Naprawia stację");
        player.sendMessage(ChatColor.YELLOW + "/stacja usun <ID_Stacji> " + ChatColor.WHITE + "- Usuwa stację");
        player.sendMessage(ChatColor.YELLOW + "/stacja info [ID_Stacji] " + ChatColor.WHITE + "- Informacje o stacji");
        player.sendMessage(ChatColor.YELLOW + "/stacja lista " + ChatColor.WHITE + "- Lista wszystkich stacji");
        player.sendMessage(ChatColor.YELLOW + "/stacja zasieg " + ChatColor.WHITE + "- Sprawdza dostępny zasięg");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("utworz", "ulepsz", "napraw", "usun", "info", "lista", "zasieg"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("utworz")) {
            completions.addAll(plugin.getOperatorManager().getAllOperators().stream()
                .map(op -> op.getId())
                .collect(Collectors.toList()));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("utworz")) {
            completions.addAll(Arrays.asList("2G", "LTE", "5G"));
        }

        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
}
