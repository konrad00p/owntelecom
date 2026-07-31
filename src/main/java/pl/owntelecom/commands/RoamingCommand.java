package pl.owntelecom.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.owntelecom.OwnTelecom;

public class RoamingCommand implements CommandExecutor {

    private final OwnTelecom plugin;

    public RoamingCommand(OwnTelecom plugin) {
        this.plugin = plugin;
        plugin.getCommand("roaming").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Ta komenda jest tylko dla graczy!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            boolean enabled = plugin.getNetworkManager().isAutoRoamingEnabled(player);
            player.sendMessage(ChatColor.GOLD + "=== Roaming ===");
            player.sendMessage(ChatColor.YELLOW + "Auto-roaming: " + 
                (enabled ? ChatColor.GREEN + "WŁĄCZONY" : ChatColor.RED + "WYŁĄCZONY"));
            player.sendMessage(ChatColor.GRAY + "/roaming wlacz - włącz automatyczny roaming");
            player.sendMessage(ChatColor.GRAY + "/roaming wylacz - wyłącz automatyczny roaming");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "wlacz":
            case "on":
                plugin.getNetworkManager().setAutoRoaming(player, true);
                break;
            case "wylacz":
            case "off":
                plugin.getNetworkManager().setAutoRoaming(player, false);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Użycie: /roaming wlacz lub /roaming wylacz");
                break;
        }

        return true;
    }
}
