package com.owntelecom.command;

import com.owntelecom.OwnTelecomPlugin;
import com.owntelecom.database.model.AgreementType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class AgreementCommand extends BaseCommand {

    public AgreementCommand(OwnTelecomPlugin plugin) {
        super(plugin, "umowa");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage("/umowa <roaming|polaczenia> <operatorB> [stawka1] [stawka2] [stawka3]");
            return true;
        }
        AgreementType type = args[0].equalsIgnoreCase("roaming") ? AgreementType.ROAMING : AgreementType.CALLS;
        var owned = plugin.getDatabaseManager().operators().findByOwner(player.getUniqueId());
        if (owned.isEmpty()) {
            plugin.getMessageService().send(player, "operator.not-owner");
            return true;
        }
        String operatorA = owned.get(0).id();
        String operatorB = args[1];
        double r1 = args.length > 2 ? Double.parseDouble(args[2]) : 0;
        double r2 = args.length > 3 ? Double.parseDouble(args[3]) : 0;
        double r3 = args.length > 4 ? Double.parseDouble(args[4]) : 0;
        plugin.getModuleManager().getAgreementModule().createAgreement(operatorA, operatorB, type, r1, r2, r3, false);
        player.sendMessage("Umowa " + type.name() + " z " + operatorB + " utworzona.");
        return true;
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("roaming", "polaczenia"), args[0]);
        }
        return super.tabComplete(sender, args);
    }
}
