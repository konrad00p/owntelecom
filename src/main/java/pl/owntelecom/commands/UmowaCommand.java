package pl.owntelecom.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.owntelecom.OwnTelecom;
import pl.owntelecom.models.Agreement;
import pl.owntelecom.models.Operator;
import pl.owntelecom.models.RoamingZone;

import java.util.*;
import java.util.stream.Collectors;

public class UmowaCommand implements CommandExecutor, TabCompleter {

    private final OwnTelecom plugin;

    public UmowaCommand(OwnTelecom plugin) {
        this.plugin = plugin;
        // Rejestracja komendy (zakładam, że dodamy ją w plugin.yml)
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
            case "zaproponuj":
            case "propose":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Użycie: /umowa zaproponuj <ID_Operatora> <typ> [strefa]");
                    player.sendMessage(ChatColor.GRAY + "Typy: CALLS, SMS, ROAMING, FULL");
                    return true;
                }
                String zoneId = args.length > 3 ? args[3] : "0";
                plugin.getAgreementManager().proposeAgreement(player, args[1], args[2], zoneId);
                break;

            case "akceptuj":
            case "accept":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użycie: /umowa akceptuj <ID_Umowy>");
                    return true;
                }
                plugin.getAgreementManager().acceptAgreement(player, args[1]);
                break;

            case "odrzuc":
            case "reject":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użycie: /umowa odrzuc <ID_Umowy>");
                    return true;
                }
                plugin.getAgreementManager().rejectAgreement(player, args[1]);
                break;

            case "zerwij":
            case "terminate":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użycie: /umowa zerwij <ID_Umowy>");
                    return true;
                }
                plugin.getAgreementManager().terminateAgreement(player, args[1]);
                break;

            case "stawka":
            case "rate":
                if (args.length < 5) {
                    player.sendMessage(ChatColor.RED + "Użycie: /umowa stawka <ID_Umowy> <AtoB/BtoA> <minuta/sms/mb> <cena>");
                    return true;
                }
                try {
                    double rate = Double.parseDouble(args[4]);
                    plugin.getAgreementManager().setAgreementRates(player, args[1], args[2], args[3], rate);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Nieprawidłowa cena!");
                }
                break;

            case "lista":
            case "list":
                handleList(player, args.length > 1 ? args[1] : null);
                break;

            case "info":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użycie: /umowa info <ID_Umowy>");
                    return true;
                }
                handleInfo(player, args[1]);
                break;

            case "strefy":
            case "zones":
                handleZones(player);
                break;

            case "roaming":
                handleRoamingStatus(player);
                break;

            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void handleList(Player player, String filter) {
        List<Agreement> agreements;
        
        if (filter != null && filter.equalsIgnoreCase("oczekujace")) {
            // Pokaż oczekujące umowy dla operatora gracza
            List<Operator> playerOps = plugin.getOperatorManager().getOperatorsByOwner(player.getUniqueId());
            agreements = new ArrayList<>();
            for (Operator op : playerOps) {
                agreements.addAll(plugin.getAgreementManager().getPendingAgreements(op.getId()));
            }
            player.sendMessage(ChatColor.GOLD + "=== Oczekujące umowy ===");
        } else {
            agreements = plugin.getAgreementManager().getActiveAgreements();
            player.sendMessage(ChatColor.GOLD + "=== Aktywne umowy ===");
        }

        if (agreements.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Brak umów.");
            return;
        }

        for (Agreement agreement : agreements) {
            String status = getStatusSymbol(agreement.getStatus());
            player.sendMessage(status + " " + ChatColor.YELLOW + agreement.getId().substring(0, 15) + "..." + 
                ChatColor.GRAY + " [" + agreement.getType() + "] " +
                agreement.getOperatorA() + " ↔ " + agreement.getOperatorB());
        }
    }

    private void handleInfo(Player player, String agreementId) {
        Agreement agreement = plugin.getAgreementManager().getAgreement(agreementId);
        if (agreement == null) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono umowy!");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "══════ Umowa ══════");
        player.sendMessage(ChatColor.YELLOW + "ID: " + ChatColor.WHITE + agreement.getId());
        player.sendMessage(ChatColor.YELLOW + "Typ: " + ChatColor.WHITE + agreement.getType());
        player.sendMessage(ChatColor.YELLOW + "Status: " + getStatusText(agreement.getStatus()));
        player.sendMessage(ChatColor.YELLOW + "Operator A: " + ChatColor.WHITE + agreement.getOperatorA());
        player.sendMessage(ChatColor.YELLOW + "Operator B: " + ChatColor.WHITE + agreement.getOperatorB());
        player.sendMessage(ChatColor.YELLOW + "Strefa: " + ChatColor.WHITE + 
            plugin.getAgreementManager().getZone(agreement.getZoneId()).getDisplayName());
        
        player.sendMessage(ChatColor.GOLD + "Stawki A→B:");
        player.sendMessage(ChatColor.GRAY + "  Minuta: $" + agreement.getRateAtoB("minuta"));
        player.sendMessage(ChatColor.GRAY + "  SMS: $" + agreement.getRateAtoB("sms"));
        player.sendMessage(ChatColor.GRAY + "  MB: $" + agreement.getRateAtoB("mb"));
        
        player.sendMessage(ChatColor.GOLD + "Stawki B→A:");
        player.sendMessage(ChatColor.GRAY + "  Minuta: $" + agreement.getRateBtoA("minuta"));
        player.sendMessage(ChatColor.GRAY + "  SMS: $" + agreement.getRateBtoA("sms"));
        player.sendMessage(ChatColor.GRAY + "  MB: $" + agreement.getRateBtoA("mb"));
    }

    private void handleZones(Player player) {
        player.sendMessage(ChatColor.GOLD + "══════ Strefy Roamingowe ══════");
        
        for (RoamingZone zone : plugin.getAgreementManager().getAllZones()) {
            String marker = zone.isHomeZone() ? ChatColor.GREEN + " 🏠 HOME" : ChatColor.YELLOW + " 🌍";
            player.sendMessage(marker + " " + ChatColor.YELLOW + zone.getDisplayName() + 
                ChatColor.GRAY + " (ID: " + zone.getId() + ")");
            player.sendMessage(ChatColor.GRAY + "  Operatorzy: " + zone.getOperatorsInZone().size());
        }
    }

    private void handleRoamingStatus(Player player) {
        player.sendMessage(ChatColor.GOLD + "══════ Status Roamingu ══════");
        
        for (Operator op : plugin.getOperatorManager().getOperatorsByOwner(player.getUniqueId())) {
            boolean roaming = plugin.getAgreementManager().isPlayerRoaming(player, op.getId());
            RoamingZone zone = plugin.getAgreementManager().getPlayerRoamingZone(player, op.getId());
            
            player.sendMessage(ChatColor.YELLOW + op.getDisplayName() + ": " + 
                (roaming ? ChatColor.GOLD + "🌍 Roaming - " + zone.getDisplayName() : 
                           ChatColor.GREEN + "🏠 Home"));
        }
    }

    private String getStatusSymbol(Agreement.AgreementStatus status) {
        switch (status) {
            case ACTIVE: return ChatColor.GREEN + "✓";
            case PENDING: return ChatColor.YELLOW + "⏳";
            case REJECTED: return ChatColor.RED + "✗";
            case TERMINATED: return ChatColor.RED + "🔒";
            case EXPIRED: return ChatColor.GRAY + "⏰";
            default: return "?";
        }
    }

    private String getStatusText(Agreement.AgreementStatus status) {
        switch (status) {
            case ACTIVE: return ChatColor.GREEN + "Aktywna";
            case PENDING: return ChatColor.YELLOW + "Oczekująca";
            case REJECTED: return ChatColor.RED + "Odrzucona";
            case TERMINATED: return ChatColor.RED + "Zerwana";
            case EXPIRED: return ChatColor.GRAY + "Wygasła";
            default: return "Nieznany";
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "══════ Umowy - Pomoc ══════");
        player.sendMessage(ChatColor.YELLOW + "/umowa zaproponuj <operator> <typ> [strefa] " + ChatColor.WHITE + "- Nowa umowa");
        player.sendMessage(ChatColor.YELLOW + "/umowa akceptuj <id> " + ChatColor.WHITE + "- Akceptuj");
        player.sendMessage(ChatColor.YELLOW + "/umowa odrzuc <id> " + ChatColor.WHITE + "- Odrzuć");
        player.sendMessage(ChatColor.YELLOW + "/umowa zerwij <id> " + ChatColor.WHITE + "- Zerwij");
        player.sendMessage(ChatColor.YELLOW + "/umowa stawka <id> <kierunek> <typ> <cena> " + ChatColor.WHITE + "- Ustaw stawkę");
        player.sendMessage(ChatColor.YELLOW + "/umowa lista [oczekujace] " + ChatColor.WHITE + "- Lista umów");
        player.sendMessage(ChatColor.YELLOW + "/umowa info <id> " + ChatColor.WHITE + "- Szczegóły");
        player.sendMessage(ChatColor.YELLOW + "/umowa strefy " + ChatColor.WHITE + "- Lista stref");
        player.sendMessage(ChatColor.YELLOW + "/umowa roaming " + ChatColor.WHITE + "- Status roamingu");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList(
                "zaproponuj", "akceptuj", "odrzuc", "zerwij", 
                "stawka", "lista", "info", "strefy", "roaming"
            ));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("zaproponuj")) {
            completions.addAll(plugin.getOperatorManager().getActiveOperators().stream()
                .map(Operator::getId)
                .collect(Collectors.toList()));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("zaproponuj")) {
            completions.addAll(Arrays.asList("CALLS", "SMS", "ROAMING", "FULL"));
        }

        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
}
