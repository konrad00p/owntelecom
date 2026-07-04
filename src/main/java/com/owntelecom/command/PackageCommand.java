package com.owntelecom.command;

import com.owntelecom.OwnTelecomPlugin;
import com.owntelecom.database.repository.PackageRepository;
import com.owntelecom.gui.PackageSelectGui;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class PackageCommand extends BaseCommand {

    public PackageCommand(OwnTelecomPlugin plugin) {
        super(plugin, "pakiet");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("lista")) {
            var sub = plugin.getDatabaseManager().subscribers().findByPlayer(player.getUniqueId());
            if (sub.isEmpty()) {
                plugin.getMessageService().send(player, "errors.no-operator");
                return true;
            }
            new PackageSelectGui(plugin, player, sub.get().operatorId()).open();
            return true;
        }
        if (args[0].equalsIgnoreCase("utworz")) {
            handleCreate(player, args);
            return true;
        }
        player.sendMessage("/pakiet <lista|utworz> ...");
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        var owned = plugin.getDatabaseManager().operators().findByOwner(player.getUniqueId());
        if (owned.isEmpty()) {
            plugin.getMessageService().send(player, "operator.not-owner");
            return;
        }
        if (args.length < 7) {
            player.sendMessage("/pakiet utworz <nazwa> <cena> <min> <sms> <mb> <dni>");
            return;
        }
        try {
            var pkg = new PackageRepository.ServicePackage(
                    0,
                    owned.get(0).id(),
                    args[1],
                    Double.parseDouble(args[2]),
                    Double.parseDouble(args[3]),
                    Double.parseDouble(args[4]),
                    Double.parseDouble(args[5]),
                    Integer.parseInt(args[6]),
                    null
            );
            plugin.getDatabaseManager().packages().create(pkg);
            player.sendMessage("Pakiet utworzony.");
        } catch (NumberFormatException e) {
            player.sendMessage("Nieprawidlowe liczby.");
        }
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("lista", "utworz"), args[0]);
        }
        return super.tabComplete(sender, args);
    }
}
