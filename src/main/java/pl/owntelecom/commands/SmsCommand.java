package pl.owntelecom.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.owntelecom.OwnTelecom;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SmsCommand implements CommandExecutor, TabCompleter {

    private final OwnTelecom plugin;

    public SmsCommand(OwnTelecom plugin) {
        this.plugin = plugin;
        plugin.getCommand("sms").setExecutor(this);
        plugin.getCommand("sms").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Ta komenda jest tylko dla graczy!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Użycie: /sms <nick> <treść>");
            return true;
        }

        Player receiver = Bukkit.getPlayer(args[0]);
        if (receiver == null || !receiver.isOnline()) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono gracza: " + args[0]);
            return true;
        }

        // Połącz pozostałe argumenty w treść SMS
        StringBuilder message = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) message.append(" ");
            message.append(args[i]);
        }

        plugin.getCallManager().sendSMS(player, receiver, message.toString());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                .filter(name -> !name.equals(sender.getName()))
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
