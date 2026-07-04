package com.owntelecom.command;

import com.owntelecom.OwnTelecomPlugin;
import com.owntelecom.database.model.Operator;
import com.owntelecom.gui.OperatorSelectGui;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OperatorCommand extends BaseCommand {

    public OperatorCommand(OwnTelecomPlugin plugin) {
        super(plugin, "operator");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("/operator <utworz|dolacz|info|usun|oddaj|przejmij|stawki|lista>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "utworz" -> handleCreate(sender, args);
            case "dolacz" -> handleJoin(sender);
            case "info" -> handleInfo(sender, args);
            case "usun" -> handleDelete(sender, args);
            case "oddaj" -> handleTransfer(sender, args, false);
            case "przejmij" -> handleTransfer(sender, args, true);
            case "stawki" -> handleRates(sender, args);
            case "lista" -> handleList(sender);
            default -> sender.sendMessage("Nieznana podkomenda.");
        }
        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return;
        }
        if (args.length < 2) {
            player.sendMessage("/operator utworz <Nazwa Wyswietlana> [id]");
            return;
        }
        String displayName = args[1];
        for (int i = 2; i < args.length - 1; i++) {
            displayName += " " + args[i];
        }
        String id = args.length >= 3 ? args[args.length - 1] : "";
        if (args.length == 2) {
            id = "";
        } else if (displayName.endsWith(" " + args[args.length - 1]) && args.length > 2) {
            // id was last arg
            int lastSpace = displayName.lastIndexOf(' ');
            id = displayName.substring(lastSpace + 1);
            displayName = displayName.substring(0, lastSpace);
        }

        Optional<Operator> created = plugin.getModuleManager().getOperatorModule()
                .createOperator(player, displayName, id);
        if (created.isPresent()) {
            plugin.getMessageService().send(player, "operator.created", Map.of(
                    "name", created.get().displayName(),
                    "id", created.get().id()
            ));
        } else {
            plugin.getMessageService().send(player, "operator.already-exists");
        }
    }

    private void handleJoin(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return;
        }
        new OperatorSelectGui(plugin, player).open();
    }

    private void handleInfo(CommandSender sender, String[] args) {
        String id = args.length >= 2 ? args[1] : null;
        if (id == null && sender instanceof Player player) {
            var sub = plugin.getDatabaseManager().subscribers().findByPlayer(player.getUniqueId());
            if (sub.isEmpty()) {
                plugin.getMessageService().send(sender, "errors.no-operator");
                return;
            }
            id = sub.get().operatorId();
        }
        if (id == null) {
            sender.sendMessage("/operator info [id]");
            return;
        }
        var op = plugin.getDatabaseManager().operators().findById(id);
        if (op.isEmpty()) {
            sender.sendMessage("Operator nie istnieje.");
            return;
        }
        Operator o = op.get();
        sender.sendMessage(plugin.getMessageService().colorize(
                "&bOperator: &f" + o.displayName() + " &7(ID: " + o.id() + ")\n" +
                        "&7Wlasciciel: &f" + Bukkit.getOfflinePlayer(o.ownerUuid()).getName() + "\n" +
                        "&7Stawki: &f" + o.prepaidMinute() + "/min, " + o.prepaidSms() + "/SMS, " + o.prepaidMb() + "/MB"
        ));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return;
        }
        if (args.length < 2) {
            player.sendMessage("/operator usun <id>");
            return;
        }
        if (plugin.getModuleManager().getOperatorModule().deleteOperator(args[1], player)) {
            plugin.getMessageService().send(player, "operator.deleted", Map.of("name", args[1]));
        } else {
            plugin.getMessageService().send(player, "operator.not-owner");
        }
    }

    private void handleTransfer(CommandSender sender, String[] args, boolean acquire) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return;
        }
        if (args.length < 3) {
            player.sendMessage("/operator " + (acquire ? "przejmij" : "oddaj") + " <id> <gracz>");
            return;
        }
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            plugin.getMessageService().send(player, "errors.player-not-found", Map.of("player", args[2]));
            return;
        }
        var op = plugin.getDatabaseManager().operators().findById(args[1]);
        if (op.isEmpty()) {
            player.sendMessage("Operator nie istnieje.");
            return;
        }
        if (!acquire && !op.get().ownerUuid().equals(player.getUniqueId()) && !player.hasPermission("owntelecom.admin")) {
            plugin.getMessageService().send(player, "operator.not-owner");
            return;
        }
        plugin.getModuleManager().getOperatorModule().transferOperator(args[1], target.getUniqueId());
        plugin.getMessageService().send(player, "operator.transferred", Map.of(
                "name", op.get().displayName(),
                "player", target.getName()
        ));
    }

    private void handleRates(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return;
        }
        if (args.length < 4) {
            player.sendMessage("/operator stawki <minuta> <sms> <mb>");
            return;
        }
        var owned = plugin.getDatabaseManager().operators().findByOwner(player.getUniqueId());
        if (owned.isEmpty()) {
            plugin.getMessageService().send(player, "operator.not-owner");
            return;
        }
        try {
            double min = Double.parseDouble(args[1]);
            double sms = Double.parseDouble(args[2]);
            double mb = Double.parseDouble(args[3]);
            plugin.getDatabaseManager().operators().updatePrepaidRates(owned.get(0).id(), min, sms, mb);
            player.sendMessage("Stawki zaktualizowane.");
        } catch (NumberFormatException e) {
            player.sendMessage("Nieprawidlowe liczby.");
        }
    }

    private void handleList(CommandSender sender) {
        for (Operator op : plugin.getModuleManager().getOperatorModule().listOperators()) {
            sender.sendMessage(op.displayName() + " (" + op.id() + ")");
        }
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("utworz", "dolacz", "info", "usun", "oddaj", "przejmij", "stawki", "lista"), args[0]);
        }
        return super.tabComplete(sender, args);
    }
}
