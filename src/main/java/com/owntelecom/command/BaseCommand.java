package com.owntelecom.command;

import com.owntelecom.OwnTelecomPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    protected final OwnTelecomPlugin plugin;
    private final String commandName;

    protected BaseCommand(OwnTelecomPlugin plugin, String commandName) {
        this.plugin = plugin;
        this.commandName = commandName;
    }

    public void register() {
        var cmd = plugin.getCommand(commandName);
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return execute(sender, args);
    }

    protected abstract boolean execute(CommandSender sender, String[] args);

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return tabComplete(sender, args);
    }

    protected List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    protected List<String> filterPrefix(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream().filter(s -> s.toLowerCase().startsWith(lower)).toList();
    }
}
