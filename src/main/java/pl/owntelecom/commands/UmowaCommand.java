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
        Agreement agreement = plugin.getAgreementManager
