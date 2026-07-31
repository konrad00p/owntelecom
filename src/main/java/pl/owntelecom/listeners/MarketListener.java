package pl.owntelecom.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pl.owntelecom.OwnTelecom;

public class MarketListener implements Listener {

    private final OwnTelecom plugin;

    public MarketListener(OwnTelecom plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        String title = event.getView().getTitle();

        // Sprawdź czy to GUI Allegro
        if (title.contains("Allegro - Rynek Globalny")) {
            event.setCancelled(true); // Zablokuj przesuwanie przedmiotów
            
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null) return;

            // Kupowanie
            if (plugin.getAllegroCommand() != null) {
                boolean success = plugin.getAllegroCommand().buyItem(player, clickedItem);
                
                if (success) {
                    player.closeInventory();
                    player.sendMessage(ChatColor.GREEN + "✅ Transakcja zakończona pomyślnie!");
                }
            }
        }
    }
}
