package com.owntelecom.command;

import com.owntelecom.OwnTelecomPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StawkiCommand extends BaseCommand {

    public StawkiCommand(OwnTelecomPlugin plugin) {
        super(plugin, "stawki");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        String operatorId = null;
        String type = "krajowe";

        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("roaming") || args[0].equalsIgnoreCase("polaczenia")) {
                type = args[0].toLowerCase();
                if (args.length >= 2) {
                    operatorId = args[1];
                }
            } else {
                operatorId = args[0];
            }
        }

        if (operatorId == null && sender instanceof Player player) {
            var sub = plugin.getDatabaseManager().subscribers().findByPlayer(player.getUniqueId());
            if (sub.isPresent()) {
                operatorId = sub.get().operatorId();
            }
        }

        if (operatorId == null) {
            sender.sendMessage("/stawki [roaming|polaczenia] [operator]");
            return true;
        }

        var op = plugin.getDatabaseManager().operators().findById(operatorId);
        if (op.isEmpty()) {
            sender.sendMessage("Operator nie istnieje.");
            return true;
        }

        sender.sendMessage(plugin.getMessageService().colorize(
                "&bStawki &7(" + type + ") &f" + op.get().displayName() + "\n" +
                        "&7/min: &f" + op.get().prepaidMinute() + "\n" +
                        "&7/SMS: &f" + op.get().prepaidSms() + "\n" +
                        "&7/MB: &f" + op.get().prepaidMb()
        ));
        return true;
    }
}
