package com.owntelecom.command;

import com.owntelecom.OwnTelecomPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class EmergencyCommand extends BaseCommand {

    public EmergencyCommand(OwnTelecomPlugin plugin) {
        super(plugin, "112");
    }

    public void register() {
        super.register();
        var alarm = plugin.getCommand("alarmowy");
        if (alarm != null) {
            alarm.setExecutor(this);
        }
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage("/112 <wiadomosc>");
            return true;
        }
        String message = String.join(" ", args);
        plugin.getModuleManager().getCallModule().sendEmergency(player, message);
        return true;
    }
}
