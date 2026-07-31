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

public class CallCommand implements CommandExecutor, TabCompleter {

    private final OwnTelecom plugin;

    public CallCommand(OwnTelecom plugin) {
        this.plugin = plugin;
        plugin.getCommand("call").setExecutor(this);
        plugin.getCommand("call").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Ta komenda jest tylko dla graczy!");
            return true;
        }

        Player caller = (Player) sender;

        if (args.length == 0) {
            caller.sendMessage(ChatColor.RED + "Użycie: /call <nick>");
            return true;
        }

        Player receiver = Bukkit.getPlayer(args[0]);
        if (receiver == null || !receiver.isOnline()) {
            caller.sendMessage(ChatColor.RED + "Nie znaleziono gracza: " + args[0]);
            return true;
        }

        plugin.getCallManager().initiateCall(caller, receiver);
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
