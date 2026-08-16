package pl.owntelecom.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.owntelecom.OwnTelecom;
import pl.owntelecom.models.Operator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OperatorCommand implements CommandExecutor, TabCompleter {

    private final OwnTelecom plugin;

    public OperatorCommand(OwnTelecom plugin) {
        this.plugin = plugin;
        plugin.getCommand("operator").setExecutor(this);
        plugin.getCommand("operator").setTabCompleter(this);
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
                    player.sendMessage(ChatColor.RED + "Użycie: /operator utworz <ID> <Nazwa Wyświetlana>");
                    return true;
                }
                handleCreate(player, args[1], args[2]);
                break;

            case "usun":
            case "delete":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użycie: /operator usun <ID>");
                    return true;
                }
                handleDelete(player, args[1]);
                break;

            case "przekaz":
            case "transfer":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Użycie: /operator przekaz <ID> <gracz>");
                    return true;
                }
                handleTransfer(player, args[1], args[2]);
                break;

            case "sprzedaj":
            case "sell":
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "Użycie: /operator sprzedaj <ID> <gracz> <cena>");
                    return true;
                }
                handleSell(player, args[1], args[2], args[3]);
                break;

            case "info":
                if (args.length < 2) {
                    handleListMyOperators(player);
                } else {
                    handleInfo(player, args[1]);
                }
                break;

            case "lista":
            case "list":
                handleList(player);
                break;

            case "stawka":
            case "rate":
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "Użycie: /operator stawka <ID> <minuta/sms/mb> <cena>");
                    return true;
                }
                handleSetRate(player, args[1], args[2], args[3]);
                break;

            case "pracownik":
            case "employee":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Użycie: /operator pracownik <dodaj/usun> <gracz> [ID_operatora]");
                    return true;
                }
                handleEmployee(player, args);
                break;

            case "pakiet":
            case "package":
                if (args.length < 2) {
                    sendPackageHelp(player);
                    return true;
                }
                if (args[1].equalsIgnoreCase("utworz") || args[1].equalsIgnoreCase("create")) {
                    handleCreatePackage(player, args);
                } else if (args[1].equalsIgnoreCase("lista") || args[1].equalsIgnoreCase("list")) {
                    if (args.length < 3) {
                        player.sendMessage(ChatColor.RED + "Użycie: /operator pakiet lista <ID_Operatora>");
                        return true;
                    }
                    handleListPackages(player, args[2]);
                } else {
                    sendPackageHelp(player);
                }
                break;

            case "pakiety":
            case "packages":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użycie: /operator pakiety <ID_Operatora>");
                    return true;
                }
                handleListPackages(player, args[1]);
                break;

            case "admin":
                if (player.hasPermission("owntelecom.admin")) {
                    handleAdmin(player, args);
                } else {
                    player.sendMessage(ChatColor.RED + "Brak uprawnień!");
                }
                break;

            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    // ==================== ISTNIEJĄCE METODY ====================

    private void handleCreate(Player player, String id, String displayName) {
        if (!id.matches("^[a-zA-Z0-9]+$")) {
            player.sendMessage(ChatColor.RED + "ID może zawierać tylko litery i cyfry!");
            return;
        }

        plugin.getOperatorManager().createOperator(player, id, displayName);
    }

    private void handleDelete(Player player, String operatorId) {
        plugin.getOperatorManager().deleteOperator(player, operatorId);
    }

    private void handleTransfer(Player player, String operatorId, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono gracza: " + targetName);
            return;
        }

        plugin.getOperatorManager().transferOperator(player, target, operatorId);
    }

    private void handleSell(Player player, String operatorId, String buyerName, String priceStr) {
        Player buyer = Bukkit.getPlayer(buyerName);
        if (buyer == null) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono gracza: " + buyerName);
            return;
        }

        try {
            double price = Double.parseDouble(priceStr);
            if (price < 0) {
                player.sendMessage(ChatColor.RED + "Cena nie może być ujemna!");
                return;
            }
            plugin.getOperatorManager().sellOperator(player, buyer, operatorId, price);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Nieprawidłowa cena!");
        }
    }

    private void handleInfo(Player player, String operatorId) {
        Operator op = plugin.getOperatorManager().getOperator(operatorId);
        if (op == null) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono operatora: " + operatorId);
            return;
        }

        showOperatorInfo(player, op);
    }

    private void handleListMyOperators(Player player) {
        List<Operator> myOperators = plugin.getOperatorManager().getOperatorsByOwner(player.getUniqueId());
        
        if (myOperators.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Nie posiadasz żadnego operatora. Utwórz go: /operator utworz <ID> <Nazwa>");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== Twoi operatorzy ===");
        for (Operator op : myOperators) {
            player.sendMessage(ChatColor.YELLOW + "• " + op.getDisplayName() + ChatColor.GRAY + " (ID: " + op.getId() + ")");
        }
    }

    private void handleList(Player player) {
        List<Operator> allOperators = plugin.getOperatorManager().getActiveOperators();
        
        player.sendMessage(ChatColor.GOLD + "=== Aktywni operatorzy ===");
        for (Operator op : allOperators) {
            player.sendMessage(ChatColor.YELLOW + "• " + op.getDisplayName() + 
                ChatColor.GRAY + " (Właściciel: " + op.getOwnerName() + ")");
        }
    }

    private void handleSetRate(Player player, String operatorId, String type, String rateStr) {
        if (!type.equalsIgnoreCase("minuta") && 
            !type.equalsIgnoreCase("sms") && 
            !type.equalsIgnoreCase("mb")) {
            player.sendMessage(ChatColor.RED + "Nieprawidłowy typ! Dostępne: minuta, sms, mb");
            return;
        }

        try {
            double rate = Double.parseDouble(rateStr);
            plugin.getOperatorManager().setRate(player, operatorId, type.toLowerCase(), rate);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Nieprawidłowa cena!");
        }
    }

    private void handleEmployee(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Użycie: /operator pracownik <dodaj/usun> <gracz> [ID_operatora]");
            return;
        }

        String action = args[1].toLowerCase();
        Player target = Bukkit.getPlayer(args[2]);
        
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono gracza: " + args[2]);
            return;
        }

        String operatorId;
        if (args.length >= 4) {
            operatorId = args[3];
        } else {
            List<Operator> myOps = plugin.getOperatorManager().getOperatorsByOwner(player.getUniqueId());
            if (myOps.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Nie posiadasz żadnego operatora!");
                return;
            }
            operatorId = myOps.get(0).getId();
        }

        if (action.equals("dodaj") || action.equals("add")) {
            plugin.getOperatorManager().addEmployee(player, operatorId, target);
        } else if (action.equals("usun") || action.equals("remove")) {
            player.sendMessage(ChatColor.RED + "Funkcja usuwania pracownika jeszcze nie zaimplementowana.");
        }
    }

    private void handleAdmin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Użycie: /operator admin <changeowner/delete/listall>");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "changeowner":
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "Użycie: /operator admin changeowner <ID_operatora> <nowy_wlasciciel>");
                    return;
                }
                Player newOwner = Bukkit.getPlayer(args[3]);
                if (newOwner == null) {
                    player.sendMessage(ChatColor.RED + "Nie znaleziono gracza!");
                    return;
                }
                plugin.getOperatorManager().adminChangeOwner(player, args[2], newOwner);
                break;

            default:
                player.sendMessage(ChatColor.RED + "Nieznana akcja admina.");
                break;
        }
    }

    private void showOperatorInfo(Player player, Operator op) {
        player.sendMessage(ChatColor.GOLD + "=== " + op.getDisplayName() + " ===");
        player.sendMessage(ChatColor.YELLOW + "ID: " + ChatColor.WHITE + op.getId());
        player.sendMessage(ChatColor.YELLOW + "Właściciel: " + ChatColor.WHITE + op.getOwnerName());
        player.sendMessage(ChatColor.YELLOW + "Strefa: " + ChatColor.WHITE + op.getZone());
        player.sendMessage(ChatColor.YELLOW + "Aktywny: " + (op.isActive() ? ChatColor.GREEN + "Tak" : ChatColor.RED + "Nie"));
        player.sendMessage(ChatColor.YELLOW + "Stawki:");
        player.sendMessage(ChatColor.GRAY + "  Minuta: $" + op.getRate("minuta"));
        player.sendMessage(ChatColor.GRAY + "  SMS: $" + op.getRate("sms"));
        player.sendMessage(ChatColor.GRAY + "  MB: $" + op.getRate("mb"));
        player.sendMessage(ChatColor.YELLOW + "Stawki roamingowe:");
        player.sendMessage(ChatColor.GRAY + "  Minuta: $" + op.getRoamingRate("minuta"));
        player.sendMessage(ChatColor.GRAY + "  SMS: $" + op.getRoamingRate("sms"));
        player.sendMessage(ChatColor.GRAY + "  MB: $" + op.getRoamingRate("mb"));
        player.sendMessage(ChatColor.YELLOW + "Umowy: " + ChatColor.WHITE + op.getAgreements().size());
        player.sendMessage(ChatColor.YELLOW + "Pracownicy: " + ChatColor.WHITE + op.getEmployees().size());
    }

    // ==================== NOWE METODY PAKIETÓW ====================

    private void handleCreatePackage(Player player, String[] args) {
        if (args.length < 10) {
            sendPackageCreateHelp(player);
            return;
        }

        String operatorId = args[2];
        String name = args[3];
        String typeStr = args[4].toUpperCase();

        try {
            double price = Double.parseDouble(args[5]);
            int days = Integer.parseInt(args[6]);
            int minutes = Integer.parseInt(args[7]);
            int sms = Integer.parseInt(args[8]);
            int megabytes = Integer.parseInt(args[9]);

            plugin.getBillingManager().createSubscription(
                player, operatorId, name, typeStr, price, days, minutes, sms, megabytes);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Nieprawidłowe liczby! Sprawdź: cena, dni, min, sms, mb.");
        }
    }

    private void handleListPackages(Player player, String operatorId) {
        var packages = plugin.getBillingManager().getOperatorSubscriptions(operatorId);

        if (packages.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Ten operator nie ma jeszcze pakietów.");
            player.sendMessage(ChatColor.GRAY + "Utwórz: /operator pakiet utworz " + operatorId + " ...");
            return;
        }

        Operator op = plugin.getOperatorManager().getOperator(operatorId);
        String opName = op != null ? op.getDisplayName() : operatorId;

        player.sendMessage(ChatColor.GOLD + "=== Pakiety: " + opName + " ===");

        for (var pkg : packages) {
            String typeIcon = pkg.getType().name().equals("ROAMING") ? "🌍" : "📱";
            player.sendMessage(typeIcon + " " + ChatColor.GREEN + pkg.getName() + 
                ChatColor.WHITE + " - $" + String.format("%.2f", pkg.getPrice()));
            player.sendMessage(ChatColor.GRAY + "  ID: " + pkg.getId() + 
                " | 📞 " + (pkg.isUnlimitedMinutes() ? "∞" : pkg.getMinutes()) +
                " | 💬 " + (pkg.isUnlimitedSms() ? "∞" : pkg.getSms()) +
                " | 🌐 " + (pkg.isUnlimitedData() ? "∞" : pkg.getMegabytes()) + "MB");
            player.sendMessage(ChatColor.GRAY + "  ⏰ " + 
                (pkg.getDurationDays() > 0 ? pkg.getDurationDays() + " dni" : "Bezterminowy") +
                " | Strefa: " + pkg.getZoneId());
        }
    }

    private void sendPackageHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Pakiety - Pomoc ===");
        player.sendMessage(ChatColor.YELLOW + "/operator pakiet utworz <ID_Op> <nazwa> <typ> <cena> <dni> <min> <sms> <mb>");
        player.sendMessage(ChatColor.GRAY + "Typ: DOMESTIC lub ROAMING | -1 = nielimitowane | 0 dni = bezterminowy");
        player.sendMessage(ChatColor.YELLOW + "/operator pakiet lista <ID_Op>");
        player.sendMessage(ChatColor.GRAY + "Przykład: /operator pakiet utworz phonifyus Podstawowy DOMESTIC 30 30 100 100 500");
    }

    private void sendPackageCreateHelp(Player player) {
        player.sendMessage(ChatColor.RED + "Za mało argumentów.");
        player.sendMessage(ChatColor.YELLOW + "/operator pakiet utworz <ID_Op> <nazwa> <typ> <cena> <dni> <min> <sms> <mb>");
        player.sendMessage(ChatColor.GRAY + "Typ: DOMESTIC lub ROAMING | -1 = nielimitowane | 0 dni = bezterminowy");
    }

    // ==================== HELP ====================

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Operator - Pomoc ===");
        player.sendMessage(ChatColor.YELLOW + "/operator utworz <ID> <Nazwa> " + ChatColor.WHITE + "- Tworzy nowego operatora");
        player.sendMessage(ChatColor.YELLOW + "/operator usun <ID> " + ChatColor.WHITE + "- Usuwa operatora");
        player.sendMessage(ChatColor.YELLOW + "/operator przekaz <ID> <gracz> " + ChatColor.WHITE + "- Przekazuje operatora");
        player.sendMessage(ChatColor.YELLOW + "/operator sprzedaj <ID> <gracz> <cena> " + ChatColor.WHITE + "- Sprzedaje operatora");
        player.sendMessage(ChatColor.YELLOW + "/operator info [ID] " + ChatColor.WHITE + "- Informacje o operatorze");
        player.sendMessage(ChatColor.YELLOW + "/operator lista " + ChatColor.WHITE + "- Lista operatorów");
        player.sendMessage(ChatColor.YELLOW + "/operator stawka <ID> <typ> <cena> " + ChatColor.WHITE + "- Ustawia stawkę");
        player.sendMessage(ChatColor.YELLOW + "/operator pracownik <dodaj/usun> <gracz> " + ChatColor.WHITE + "- Zarządza pracownikami");
        player.sendMessage(ChatColor.YELLOW + "/operator pakiet utworz <...> " + ChatColor.WHITE + "- Tworzy pakiet");
        player.sendMessage(ChatColor.YELLOW + "/operator pakiety <ID> " + ChatColor.WHITE + "- Lista pakietów");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("utworz");
            completions.add("usun");
            completions.add("przekaz");
            completions.add("sprzedaj");
            completions.add("info");
            completions.add("lista");
            completions.add("stawka");
            completions.add("pracownik");
            completions.add("pakiet");
            completions.add("pakiety");
            if (sender.hasPermission("owntelecom.admin")) {
                completions.add("admin");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            completions.addAll(plugin.getOperatorManager().getAllOperators().stream()
                .map(Operator::getId)
                .collect(Collectors.toList()));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("pakiet")) {
            completions.add("utworz");
            completions.add("lista");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("pakiety")) {
            completions.addAll(plugin.getOperatorManager().getAllOperators().stream()
                .map(Operator::getId)
                .collect(Collectors.toList()));
        }
        
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
}
