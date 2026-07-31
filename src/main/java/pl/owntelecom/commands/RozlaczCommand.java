package pl.owntelecom.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.owntelecom.OwnTelecom;

public class RozlaczCommand implements CommandExecutor {

    private final OwnTelecom plugin;

    public RozlaczCommand(OwnTelecom plugin) {
        this.plugin = plugin;
        plugin.getCommand("rozlacz").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Ta komenda jest tylko dla graczy!");
            return true;
        }

        Player player = (Player) sender;
        
        // Najpierw sprawdź czy jest oczekujące połączenie do odrzucenia
        if (!plugin.getCallManager().endCall(player)) {
            // Jeśli nie, spróbuj odrzucić przychodzące
            plugin.getCallManager().rejectCall(player);
        }
        
        return true;
    }
}
