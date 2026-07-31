package pl.owntelecom.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.owntelecom.OwnTelecom;

public class OdbierzCommand implements CommandExecutor {

    private final OwnTelecom plugin;

    public OdbierzCommand(OwnTelecom plugin) {
        this.plugin = plugin;
        plugin.getCommand("odbierz").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Ta komenda jest tylko dla graczy!");
            return true;
        }

        Player player = (Player) sender;
        plugin.getCallManager().acceptCall(player);
        return true;
    }
}
