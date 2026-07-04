package com.owntelecom.command;

import com.owntelecom.OwnTelecomPlugin;
import com.owntelecom.module.call.CallModule;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Map; // TUTAJ BYŁA TA ZBRODNIA! Dodane, załatane, zrobione.

public class CallCommand extends BaseCommand {

    public CallCommand(OwnTelecomPlugin plugin) {
        super(plugin, "call");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return true;
        }
        CallModule calls = plugin.getModuleManager().getCallModule();

        if (args.length >= 1 && args[0].equalsIgnoreCase("koniec")) {
            calls.endCall(player.getUniqueId());
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("/call <gracz> [confirm]");
            return true;
        }

        boolean confirm = args.length >= 2 && args[args.length - 1].equalsIgnoreCase("confirm");
        String targetName = confirm && args.length >= 2
                ? String.join(" ", Arrays.copyOfRange(args, 0, args.length - 1))
                : args[0];

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            plugin.getMessageService().send(player, "errors.player-not-found", Map.of("player", targetName));
            return true;
        }

        if (confirm) {
            var pending = calls.consumePending(player.getUniqueId());
            if (pending.isPresent() && pending.get().type().equals("call")) {
                calls.startCall(player, target, true);
                return true;
            }
        }

        calls.startCall(player, target, false);
        return true;
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterPrefix(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[0]);
        }
        return super.tabComplete(sender, args);
    }
}
