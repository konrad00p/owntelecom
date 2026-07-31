package pl.owntelecom.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import pl.owntelecom.OwnTelecom;
import pl.owntelecom.managers.ConfigManager;

import java.util.HashSet;
import java.util.Set;

public class ChatListener implements Listener {

    private final OwnTelecom plugin;
    private final ConfigManager configManager;
    
    // Przechowuje ID graczy, którzy mają włączony chat globalny (admin)
    private final Set<Player> globalChatPlayers = new HashSet<>();

    public ChatListener(OwnTelecom plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        String message = event.getMessage();
        
        // Sprawdź czy chat globalny jest włączony przez admina
        if (configManager.isGlobalChatEnabled() && !globalChatPlayers.contains(sender)) {
            // Chat globalny aktywny - normalne wysyłanie do wszystkich
            return;
        }
        
        // Jeśli gracz ma włączony chat globalny indywidualnie
        if (globalChatPlayers.contains(sender)) {
            event.setFormat(ChatColor.GOLD + "[Globalny] " + ChatColor.RESET + sender.getName() + ": " + message);
            return;
        }
        
        // Sprawdź czy chat lokalny jest włączony
        if (!configManager.isLocalChatEnabled()) {
            return; // Normalny chat jeśli lokalny wyłączony
        }
        
        int radius = configManager.getChatRadius();
        
        // Anuluj domyślne wysyłanie do wszystkich
        event.setCancelled(true);
        
        // Znajdź odbiorców w zasięgu
        Set<Player> recipients = new HashSet<>();
        
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.getWorld().equals(sender.getWorld())) {
                double distance = sender.getLocation().distance(target.getLocation());
                if (distance <= radius) {
                    recipients.add(target);
                }
            }
        }
        
        // Jeśli nie ma nikogo w zasięgu
        if (recipients.isEmpty()) {
            String noListenerMsg = configManager.getNoListenerMessage();
            sender.sendMessage(noListenerMsg);
            
            // Wyślij wiadomość tylko do nadawcy (echo)
            String localFormat = ChatColor.GRAY + "[Lokalny] " + ChatColor.RESET + 
                                sender.getName() + ": " + message;
            sender.sendMessage(localFormat);
        } else {
            // Wyślij wiadomość do wszystkich w zasięgu
            String localFormat = ChatColor.GRAY + "[Lokalny] " + ChatColor.RESET + 
                                sender.getName() + ": " + message;
            
            for (Player recipient : recipients) {
                recipient.sendMessage(localFormat);
            }
            
            // Log do konsoli (opcjonalnie)
            plugin.getLogger().info("[Lokalny][" + sender.getWorld().getName() + "] " + 
                                   sender.getName() + ": " + message + 
                                   " (Odbiorców: " + recipients.size() + ")");
        }
    }

    // Metoda do przełączania chatu globalnego dla gracza
    public boolean toggleGlobalChat(Player player) {
        if (globalChatPlayers.contains(player)) {
            globalChatPlayers.remove(player);
            return false; // Wyłączono
        } else {
            globalChatPlayers.add(player);
            return true; // Włączono
        }
    }
    
    public boolean hasGlobalChat(Player player) {
        return globalChatPlayers.contains(player);
    }
    
    public void removeGlobalChat(Player player) {
        globalChatPlayers.remove(player);
    }
}
