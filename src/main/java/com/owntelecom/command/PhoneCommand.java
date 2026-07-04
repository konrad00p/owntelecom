package com.owntelecom.command;

import com.owntelecom.OwnTelecomPlugin;
import com.owntelecom.service.CoverageResult;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class PhoneCommand extends BaseCommand {

    public PhoneCommand(OwnTelecomPlugin plugin) {
        super(plugin, "telefon");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            showInfo(player);
            return true;
        }
        player.sendMessage("/telefon info");
        return true;
    }

    private void showInfo(Player player) {
        var sub = plugin.getDatabaseManager().subscribers().findByPlayer(player.getUniqueId());
        if (sub.isEmpty()) {
            plugin.getMessageService().send(player, "errors.no-operator");
            return;
        }
        var op = plugin.getDatabaseManager().operators().findById(sub.get().operatorId());
        CoverageResult cov = plugin.getModuleManager().getCoverageService().getCoverage(player);

        player.sendMessage(plugin.getMessageService().colorize("&b=== Telefon ==="));
        op.ifPresent(o -> player.sendMessage(plugin.getMessageService().colorize(
                "&7Operator: &f" + o.displayName() + " &7(" + o.id() + ")"
        )));
        if (cov.inCoverage()) {
            player.sendMessage(plugin.getMessageService().colorize(
                    "&7Zasięg: &aTAK &7(" + (cov.roaming() ? "roaming" : "domowy") + ")\n" +
                            "&7Technologia: &f" + cov.technology() + "\n" +
                            "&7Predkosc: &f" + String.format("%.1f", cov.speedMbps()) + " Mb/s"
            ));
        } else {
            player.sendMessage(plugin.getMessageService().colorize("&7Zasięg: &cBRAK"));
        }
        if (sub.get().hasActivePackage(System.currentTimeMillis())) {
            player.sendMessage(plugin.getMessageService().colorize(
                    "&7Pakiet: &f" + sub.get().packageMinutesLeft() + " min, " +
                            sub.get().packageSmsLeft() + " SMS, " + sub.get().packageMbLeft() + " MB"
            ));
        }
    }
}
