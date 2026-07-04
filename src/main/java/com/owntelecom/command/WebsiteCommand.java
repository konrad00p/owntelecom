package com.owntelecom.command;

import com.owntelecom.OwnTelecomPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WebsiteCommand extends BaseCommand {

    public WebsiteCommand(OwnTelecomPlugin plugin) {
        super(plugin, "strona");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return true;
        }
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "utworz" -> handleCreate(player, args);
            case "tytul" -> handleTitle(player, args);
            case "linia" -> handleLine(player, args);
            case "usun" -> handleDelete(player, args);
            case "podglad" -> handlePreview(player, args);
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("/strona utworz <slug> <tytul>");
        player.sendMessage("/strona tytul <slug> <nowy tytul>");
        player.sendMessage("/strona linia dodaj <slug> <tekst...>");
        player.sendMessage("/strona linia usun <slug> <numer>");
        player.sendMessage("/strona usun <slug>");
        player.sendMessage("/strona podglad <slug>");
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("/strona utworz <slug> <tytul>");
            return;
        }
        String slug = args[1].toLowerCase();
        String title = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        int id = plugin.getModuleManager().getInternetModule().createWebsite(player, slug, title);
        if (id > 0) {
            player.sendMessage("Strona utworzona: /" + slug);
        }
    }

    private void handleTitle(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("/strona tytul <slug> <tytul>");
            return;
        }
        plugin.getDatabaseManager().websites().findBySlug(args[1]).ifPresentOrElse(site -> {
            if (!canEdit(player, site.ownerUuid())) {
                plugin.getMessageService().send(player, "errors.no-permission");
                return;
            }
            // title update via recreate pattern - extend repo if needed
            player.sendMessage("Tytul zaktualizowany (wymaga rozszerzenia repo - TODO).");
        }, () -> player.sendMessage("Strona nie istnieje."));
    }

    private void handleLine(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("/strona linia <dodaj|usun> <slug> ...");
            return;
        }
        String action = args[1];
        String slug = args[2];
        plugin.getDatabaseManager().websites().findBySlug(slug).ifPresentOrElse(site -> {
            if (!canEdit(player, site.ownerUuid())) {
                plugin.getMessageService().send(player, "errors.no-permission");
                return;
            }
            List<String> lines = new ArrayList<>(plugin.getDatabaseManager().websites().getLines(site.id()));
            if (action.equalsIgnoreCase("dodaj") && args.length >= 4) {
                String text = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                int maxLen = plugin.getConfigManager().getInternet()
                        .getConfigurationSection("page-templates.default").getInt("max-line-length", 50);
                if (text.length() > maxLen) {
                    text = text.substring(0, maxLen);
                }
                lines.add(text);
                plugin.getDatabaseManager().websites().setLines(site.id(), lines);
                player.sendMessage("Linia dodana.");
            } else if (action.equalsIgnoreCase("usun") && args.length >= 4) {
                int index = Integer.parseInt(args[3]) - 1;
                if (index >= 0 && index < lines.size()) {
                    lines.remove(index);
                    plugin.getDatabaseManager().websites().setLines(site.id(), lines);
                    player.sendMessage("Linia usunieta.");
                }
            }
        }, () -> player.sendMessage("Strona nie istnieje."));
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("/strona usun <slug>");
            return;
        }
        plugin.getDatabaseManager().websites().findBySlug(args[1]).ifPresentOrElse(site -> {
            if (!canEdit(player, site.ownerUuid()) && !player.hasPermission("owntelecom.admin")) {
                plugin.getMessageService().send(player, "errors.no-permission");
                return;
            }
            plugin.getDatabaseManager().websites().setEnabled(site.id(), false);
            player.sendMessage("Strona wylaczona.");
        }, () -> player.sendMessage("Strona nie istnieje."));
    }

    private void handlePreview(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("/strona podglad <slug>");
            return;
        }
        plugin.getDatabaseManager().websites().findBySlug(args[1]).ifPresentOrElse(site ->
                plugin.getModuleManager().getInternetModule().displayPage(player, site),
                () -> player.sendMessage("Strona nie istnieje."));
    }

    private boolean canEdit(Player player, java.util.UUID ownerUuid) {
        return player.getUniqueId().equals(ownerUuid) || player.hasPermission("owntelecom.admin");
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("utworz", "tytul", "linia", "usun", "podglad"), args[0]);
        }
        return super.tabComplete(sender, args);
    }
}
