package com.owntelecom.command;

import com.owntelecom.OwnTelecomPlugin;
import com.owntelecom.gui.InternetBrowserGui;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InternetCommand extends BaseCommand {

    public InternetCommand(OwnTelecomPlugin plugin) {
        super(plugin, "internet");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("tweet")) {
            if (args.length < 2) {
                player.sendMessage("/internet tweet <wiadomosc>");
                return true;
            }
            String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            plugin.getModuleManager().getInternetModule().postSocialMedia(player, message);
            return true;
        }
        new InternetBrowserGui(plugin, player).open();
        return true;
    }
}
