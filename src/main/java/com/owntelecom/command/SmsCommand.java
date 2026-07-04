package com.owntelecom.command;

import com.owntelecom.OwnTelecomPlugin;
import com.owntelecom.module.call.CallModule;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class SmsCommand extends BaseCommand {

    public SmsCommand(OwnTelecomPlugin plugin) {
        super(plugin, "sms");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage("/sms <gracz> <wiadomosc...> [confirm]");
            return true;
        }

        boolean confirm = args[args.length - 1].equalsIgnoreCase("confirm");
        String[] effectiveArgs = confirm ? Arrays.copyOf(args, args.length - 1) : args;
        Player target = Bukkit.getPlayer(effectiveArgs[0]);
        if (target == null) {
            plugin.getMessageService().send(player, "errors.player-not-found", java.util.Map.of("player", effectiveArgs[0]));
            return true;
        }
        String message = String.join(" ", Arrays.copyOfRange(effectiveArgs, 1, effectiveArgs.length));

        CallModule calls = plugin.getModuleManager().getCallModule();
        if (confirm) {
            var pending = calls.consumePending(player.getUniqueId());
            if (pending.isPresent() && pending.get().type().equals("sms")) {
                calls.sendSms(player, target, pending.get().message(), true);
                return true;
            }
        }
        calls.sendSms(player, target, message, false);
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
