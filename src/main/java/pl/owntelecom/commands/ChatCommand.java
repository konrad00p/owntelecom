package pl.owntelecom.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.owntelecom.OwnTelecom;
import pl.owntelecom.listeners.ChatListener;

public class ChatCommand implements CommandExecutor {

    private final OwnTelecom plugin;
    private final ChatListener chatListener;

    public ChatCommand(OwnTelecom plugin, ChatListener chatListener) {
        this.plugin = plugin;
        this.chatListener = chatListener;
        // Rejestracja komendy
        plugin.getCommand("chat").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Ta komenda jest tylko dla graczy!");
            return true;
        }

        Player player = (Player) sender;

        // Sprawdź uprawnienia
        if (!player.hasPermission("owntelecom.admin")) {
            player.sendMessage(ChatColor.RED + "Nie masz uprawnień do zarządzania chatem!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "wlacz":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użycie: /chat wlacz <globalny/lokalny>");
                    return true;
                }
                handleEnable(player, args[1]);
                break;

            case "wylacz":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użycie: /chat wylacz <globalny/lokalny>");
                    return true;
                }
                handleDisable(player, args[1]);
                break;

            case "status":
                showStatus(player);
                break;

            case "promien":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użycie: /chat promien <liczba>");
                    return true;
                }
                handleRadius(player, args[1]);
                break;

            case "toggle":
                handleToggle(player);
                break;

            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void handleEnable(Player player, String type) {
        switch (type.toLowerCase()) {
            case "globalny":
                plugin.getConfig().set("chat.globalny.enabled", true);
                plugin.saveConfig();
                plugin.getConfigManager().reload();
                player.sendMessage(ChatColor.GREEN + "Chat globalny został WŁĄCZONY!");
                Bukkit.broadcastMessage(ChatColor.GOLD + "[Chat] Chat globalny został włączony przez administratora!");
                break;

            case "lokalny":
                plugin.getConfig().set("chat.lokalny.enabled", true);
                plugin.saveConfig();
                plugin.getConfigManager().reload();
                player.sendMessage(ChatColor.GREEN + "Chat lokalny został WŁĄCZONY!");
                break;

            default:
                player.sendMessage(ChatColor.RED + "Nieznany typ chatu! Dostępne: globalny, lokalny");
                break;
        }
    }

    private void handleDisable(Player player, String type) {
        switch (type.toLowerCase()) {
            case "globalny":
                plugin.getConfig().set("chat.globalny.enabled", false);
                plugin.saveConfig();
                plugin.getConfigManager().reload();
                player.sendMessage(ChatColor.GREEN + "Chat globalny został WYŁĄCZONY!");
                Bukkit.broadcastMessage(ChatColor.GOLD + "[Chat] Chat globalny został wyłączony! Aktywny jest chat lokalny.");
                break;

            case "lokalny":
                plugin.getConfig().set("chat.lokalny.enabled", false);
                plugin.saveConfig();
                plugin.getConfigManager().reload();
                player.sendMessage(ChatColor.GREEN + "Chat lokalny został WYŁĄCZONY!");
                Bukkit.broadcastMessage(ChatColor.GOLD + "[Chat] Chat lokalny został wyłączony! Aktywny jest normalny chat.");
                break;

            default:
                player.sendMessage(ChatColor.RED + "Nieznany typ chatu! Dostępne: globalny, lokalny");
                break;
        }
    }

    private void showStatus(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Status Chatu ===");
        
        boolean localEnabled = plugin.getConfigManager().isLocalChatEnabled();
        boolean globalEnabled = plugin.getConfigManager().isGlobalChatEnabled();
        int radius = plugin.getConfigManager().getChatRadius();
        
        player.sendMessage(ChatColor.YELLOW + "Chat lokalny: " + 
            (localEnabled ? ChatColor.GREEN + "WŁĄCZONY" : ChatColor.RED + "WYŁĄCZONY"));
        player.sendMessage(ChatColor.YELLOW + "Chat globalny: " + 
            (globalEnabled ? ChatColor.GREEN + "WŁĄCZONY" : ChatColor.RED + "WYŁĄCZONY"));
        player.sendMessage(ChatColor.YELLOW + "Promień chatu lokalnego: " + ChatColor.WHITE + radius + " bloków");
        
        if (chatListener.hasGlobalChat(player)) {
            player.sendMessage(ChatColor.YELLOW + "Twój chat: " + ChatColor.GREEN + "GLOBALNY");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Twój chat: " + ChatColor.WHITE + "LOKALNY");
        }
    }

    private void handleRadius(Player player, String radiusStr) {
        try {
            int radius = Integer.parseInt(radiusStr);
            if (radius < 1 || radius > 1000) {
                player.sendMessage(ChatColor.RED + "Promień musi być między 1 a 1000!");
                return;
            }
            
            plugin.getConfig().set("chat.lokalny.promien", radius);
            plugin.saveConfig();
            plugin.getConfigManager().reload();
            player.sendMessage(ChatColor.GREEN + "Promień chatu lokalnego ustawiony na: " + radius + " bloków");
            
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Podaj prawidłową liczbę!");
        }
    }

    private void handleToggle(Player player) {
        boolean isGlobal = chatListener.toggleGlobalChat(player);
        
        if (isGlobal) {
            player.sendMessage(ChatColor.GREEN + "Przełączono na chat GLOBALNY.");
        } else {
            player.sendMessage(ChatColor.GREEN + "Przełączono na chat LOKALNY.");
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Chat - Pomoc ===");
        player.sendMessage(ChatColor.YELLOW + "/chat wlacz <globalny/lokalny> " + ChatColor.WHITE + "- Włącza typ chatu");
        player.sendMessage(ChatColor.YELLOW + "/chat wylacz <globalny/lokalny> " + ChatColor.WHITE + "- Wyłącza typ chatu");
        player.sendMessage(ChatColor.YELLOW + "/chat status " + ChatColor.WHITE + "- Pokazuje status chatu");
        player.sendMessage(ChatColor.YELLOW + "/chat promien <liczba> " + ChatColor.WHITE + "- Ustawia promień chatu lokalnego");
        player.sendMessage(ChatColor.YELLOW + "/chat toggle " + ChatColor.WHITE + "- Przełącza twój chat globalny/lokalny");
    }
}
