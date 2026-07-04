package com.owntelecom.command;

import com.owntelecom.OwnTelecomPlugin;
import org.bukkit.command.CommandSender;

import java.util.List;

public class OwnTelecomCommand extends BaseCommand {

    public OwnTelecomCommand(OwnTelecomPlugin plugin) {
        super(plugin, "owntelecom");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("owntelecom.admin")) {
            plugin.getMessageService().send(sender, "errors.no-permission");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("OwnTelecom - /owntelecom <reload|globalchat>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.getConfigManager().reloadAll();
                plugin.getModuleManager().getStationIndex().rebuild();
                sender.sendMessage("OwnTelecom przeładowany.");
            }
            case "globalchat" -> {
                boolean enable = args.length < 2 || !args[1].equalsIgnoreCase("off");
                plugin.getModuleManager().getChatModule().setGlobalOverride(enable);
                plugin.getMessageService().send(sender, enable ? "global-chat.enabled" : "global-chat.disabled");
            }
            default -> sender.sendMessage("Nieznana podkomenda.");
        }
        return true;
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("reload", "globalchat"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("globalchat")) {
            return filterPrefix(List.of("on", "off"), args[1]);
        }
        return super.tabComplete(sender, args);
    }
}
