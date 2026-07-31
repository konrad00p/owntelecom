package pl.owntelecom.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.owntelecom.OwnTelecom;

public class AlarmowyCommand implements CommandExecutor {

    private final OwnTelecom plugin;

    public AlarmowyCommand(OwnTelecom plugin) {
        this.plugin = plugin;
        plugin.getCommand("alarmowy").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Ta komenda jest tylko dla graczy!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Użycie: /alarmowy <treść zgłoszenia>");
            player.sendMessage(ChatColor.GRAY + "Lub użyj skrótu: /112 <treść>");
            return true;
        }

        // Połącz argumenty w treść zgłoszenia
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) message.append(" ");
            message.append(args[i]);
        }

        plugin.getCallManager().sendAlarm(player, message.toString());
        return true;
    }
}
