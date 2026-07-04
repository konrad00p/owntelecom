package com.owntelecom.command;

import com.owntelecom.OwnTelecomPlugin;
import com.owntelecom.database.model.Operator;
import com.owntelecom.database.model.Station;
import com.owntelecom.gui.StationCreateGui;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StationCommand extends BaseCommand {

    public StationCommand(OwnTelecomPlugin plugin) {
        super(plugin, "stacja");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage("/stacja <utworz|ulepsz|napraw|info> ...");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "utworz" -> handleCreate(player, args);
            case "ulepsz" -> handleUpgrade(player, args);
            case "napraw" -> handleRepair(player, args);
            case "info" -> handleInfo(player, args);
            default -> player.sendMessage("Nieznana podkomenda.");
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("/stacja utworz <operatorId>");
            return;
        }
        Optional<Operator> op = plugin.getDatabaseManager().operators().findById(args[1]);
        if (op.isEmpty()) {
            player.sendMessage("Operator nie istnieje.");
            return;
        }
        if (!op.get().ownerUuid().equals(player.getUniqueId()) && !player.hasPermission("owntelecom.admin")) {
            plugin.getMessageService().send(player, "operator.not-owner");
            return;
        }
        new StationCreateGui(plugin, player, op.get()).open();
    }

    private void handleUpgrade(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("/stacja ulepsz <id>");
            return;
        }
        int id = Integer.parseInt(args[1]);
        Optional<Station> station = plugin.getDatabaseManager().stations().findById(id);
        if (station.isEmpty()) {
            player.sendMessage("Stacja nie istnieje.");
            return;
        }
        if (plugin.getModuleManager().getStationModule().upgradeStation(player, station.get())) {
            plugin.getMessageService().send(player, "station.upgraded", Map.of("level", String.valueOf(station.get().level() + 1)));
        }
    }

    private void handleRepair(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("/stacja napraw <id>");
            return;
        }
        int id = Integer.parseInt(args[1]);
        Optional<Station> station = plugin.getDatabaseManager().stations().findById(id);
        if (station.isEmpty()) {
            player.sendMessage("Stacja nie istnieje.");
            return;
        }
        if (plugin.getModuleManager().getStationModule().repairStation(player, station.get())) {
            plugin.getMessageService().send(player, "station.repaired");
        }
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("/stacja info <id>");
            return;
        }
        int id = Integer.parseInt(args[1]);
        plugin.getDatabaseManager().stations().findById(id).ifPresentOrElse(st -> {
            player.sendMessage("Stacja #" + st.id() + " - " + st.technology() + " L" + st.level() +
                    (st.broken() ? " [AWARIA]" : ""));
        }, () -> player.sendMessage("Stacja nie istnieje."));
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("utworz", "ulepsz", "napraw", "info"), args[0]);
        }
        return super.tabComplete(sender, args);
    }
}
