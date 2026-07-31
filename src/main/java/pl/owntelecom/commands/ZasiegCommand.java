package pl.owntelecom.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.owntelecom.OwnTelecom;

public class ZasiegCommand implements CommandExecutor {

    private final OwnTelecom plugin;

    public ZasiegCommand(OwnTelecom plugin) {
        this.plugin = plugin;
        plugin.getCommand("zasieg").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Ta komenda jest tylko dla graczy!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("all")) {
            // /zasieg all lub /zasiegall
            plugin.getNetworkManager().showAllNetworks(player);
        } else {
            // /zasieg - obecny operator
            plugin.getNetworkManager().showCurrentNetwork(player);
        }

        return true;
    }
}
