package pl.owntelecom.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.owntelecom.OwnTelecom;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AllegroCommand implements CommandExecutor, TabCompleter {

    private final OwnTelecom plugin;
    // Globalny rynek: ItemStack -> (sprzedawca, cena)
    private final Map<ItemStack, AllegroListing> marketListings;
    private final double MARKET_FEE = 0.10; // 10% prowizji dla serwera

    public AllegroCommand(OwnTelecom plugin) {
        this.plugin = plugin;
        this.marketListings = new ConcurrentHashMap<>();
        plugin.getCommand("allegro").setExecutor(this);
        plugin.getCommand("allegro").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Ta komenda jest tylko dla graczy!");
            return true;
        }

        Player player = (Player) sender;

        // Sprawdź internet
        if (!plugin.getInternetManager().hasInternetAccess(player)) {
            player.sendMessage(ChatColor.RED + "📡 Potrzebujesz dostępu do internetu!");
            return true;
        }

        if (args.length == 0) {
            openMarketGUI(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "wystaw":
            case "sell":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użycie: /allegro wystaw <cena>");
                    player.sendMessage(ChatColor.GRAY + "Trzymaj przedmiot w ręce!");
                    return true;
                }
                handleListItem(player, args[1]);
                break;

            case "moje":
            case "my":
                handleMyListings(player);
                break;

            case "anuluj":
            case "cancel":
                // Anulowanie wystawienia (po indeksie)
                player.sendMessage(ChatColor.RED + "Funkcja w przygotowaniu...");
                break;

            default:
                openMarketGUI(player);
                break;
        }

        return true;
    }

    // Otwórz GUI rynku
    private void openMarketGUI(Player player) {
        List<Map.Entry<ItemStack, AllegroListing>> listings = new ArrayList<>(marketListings.entrySet());
        
        int size = Math.min(54, ((listings.size() / 9) + 1) * 9);
        if (size < 9) size = 9;
        
        Inventory gui = Bukkit.createInventory(null, size, ChatColor.GOLD + "🛒 Allegro - Rynek Globalny");
        
        int slot = 0;
        for (Map.Entry<ItemStack, AllegroListing> entry : listings) {
            if (slot >= 53) break;
            
            ItemStack displayItem = entry.getKey().clone();
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore() != null ? meta.getLore() : new ArrayList<>();
                lore.add("");
                lore.add(ChatColor.YELLOW + "Sprzedawca: " + ChatColor.WHITE + entry.getValue().sellerName);
                lore.add(ChatColor.GREEN + "Cena: $" + String.format("%.2f", entry.getValue().price));
                lore.add(ChatColor.GRAY + "Prowizja: " + (int)(MARKET_FEE * 100) + "%");
                lore.add("");
                lore.add(ChatColor.AQUA + "Kliknij LPM aby kupić!");
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }
            
            gui.setItem(slot, displayItem);
            slot++;
        }
        
        // Informacja jeśli pusto
        if (listings.isEmpty()) {
            ItemStack emptyInfo = new ItemStack(Material.PAPER);
            ItemMeta meta = emptyInfo.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GRAY + "Brak ofert");
                meta.setLore(Arrays.asList(
                    ChatColor.YELLOW + "Wystaw przedmiot:",
                    ChatColor.WHITE + "/allegro wystaw <cena>"
                ));
                emptyInfo.setItemMeta(meta);
            }
            gui.setItem(4, emptyInfo);
        }
        
        player.openInventory(gui);
    }

    // Wystaw przedmiot
    private void handleListItem(Player player, String priceStr) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Musisz trzymać przedmiot w ręce!");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price <= 0) {
                player.sendMessage(ChatColor.RED + "Cena musi być większa od 0!");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Nieprawidłowa cena!");
            return;
        }

        // Sprawdź limit wystawień (max 5 na gracza)
        long playerListings = marketListings.values().stream()
            .filter(l -> l.sellerId.equals(player.getUniqueId()))
            .count();
        
        if (playerListings >= 5 && !player.hasPermission("owntelecom.admin")) {
            player.sendMessage(ChatColor.RED + "Osiągnąłeś limit 5 wystawień!");
            return;
        }

        ItemStack listingItem = heldItem.clone();
        listingItem.setAmount(1);
        
        AllegroListing listing = new AllegroListing(
            player.getUniqueId(),
            player.getName(),
            price,
            System.currentTimeMillis()
        );
        
        marketListings.put(listingItem, listing);
        
        // Usuń przedmiot z ręki gracza
        heldItem.setAmount(heldItem.getAmount() - 1);
        
        player.sendMessage(ChatColor.GREEN + "✅ Wystawiono przedmiot na Allegro!");
        player.sendMessage(ChatColor.YELLOW + "Cena: $" + String.format("%.2f", price));
        player.sendMessage(ChatColor.GRAY + "Prowizja serwera: " + (int)(MARKET_FEE * 100) + "%");
    }

    // Pokaż swoje wystawienia
    private void handleMyListings(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Twoje wystawienia na Allegro ===");
        
        int index = 1;
        boolean found = false;
        
        for (Map.Entry<ItemStack, AllegroListing> entry : marketListings.entrySet()) {
            if (entry.getValue().sellerId.equals(player.getUniqueId())) {
                player.sendMessage(ChatColor.YELLOW + "#" + index + " " + 
                    ChatColor.WHITE + entry.getKey().getType().name() + 
                    ChatColor.GREEN + " $" + String.format("%.2f", entry.getValue().price));
                found = true;
                index++;
            }
        }
        
        if (!found) {
            player.sendMessage(ChatColor.GRAY + "Nie masz żadnych aktywnych wystawień.");
        }
    }

    // Kupowanie z GUI (wywoływane z listenera)
    public boolean buyItem(Player buyer, ItemStack clickedItem) {
        AllegroListing listing = marketListings.get(clickedItem);
        if (listing == null) return false;

        // Nie można kupić własnego przedmiotu
        if (listing.sellerId.equals(buyer.getUniqueId())) {
            buyer.sendMessage(ChatColor.RED + "Nie możesz kupić własnego przedmiotu!");
            return false;
        }

        double totalPrice = listing.price;
        double serverFee = totalPrice * MARKET_FEE;
        double sellerEarns = totalPrice - serverFee;

        // Sprawdź pieniądze
        if (!plugin.getEconomy().has(buyer, totalPrice)) {
            buyer.sendMessage(ChatColor.RED + "Nie masz wystarczających środków! Potrzebujesz: $" + 
                String.format("%.2f", totalPrice));
            return false;
        }

        // Sprawdź miejsce w ekwipunku
        if (buyer.getInventory().firstEmpty() == -1) {
            buyer.sendMessage(ChatColor.RED + "Nie masz miejsca w ekwipunku!");
            return false;
        }

        // Przeprowadź transakcję
        plugin.getEconomy().withdrawPlayer(buyer, totalPrice);
        
        // Wypłać sprzedawcy
        org.bukkit.OfflinePlayer seller = plugin.getServer().getOfflinePlayer(listing.sellerId);
        plugin.getEconomy().depositPlayer(seller, sellerEarns);
        
        // Daj przedmiot kupującemu
        buyer.getInventory().addItem(clickedItem.clone());
        
        // Usuń z rynku
        marketListings.remove(clickedItem);
        
        buyer.sendMessage(ChatColor.GREEN + "✅ Kupiono " + clickedItem.getType().name() + 
                          " za $" + String.format("%.2f", totalPrice));
        buyer.sendMessage(ChatColor.GRAY + "Prowizja: $" + String.format("%.2f", serverFee));
        
        // Powiadom sprzedawcę
        Player sellerOnline = plugin.getServer().getPlayer(listing.sellerId);
        if (sellerOnline != null && sellerOnline.isOnline()) {
            sellerOnline.sendMessage(ChatColor.GREEN + "💰 " + buyer.getName() + 
                " kupił Twój przedmiot z Allegro!");
            sellerOnline.sendMessage(ChatColor.YELLOW + "Zarobiłeś: $" + String.format("%.2f", sellerEarns));
        }
        
        return true;
    }

    // Klasa wewnętrzna dla ogłoszeń
    private static class AllegroListing {
        final UUID sellerId;
        final String sellerName;
        final double price;
        final long listingDate;

        AllegroListing(UUID sellerId, String sellerName, double price, long listingDate) {
            this.sellerId = sellerId;
            this.sellerName = sellerName;
            this.price = price;
            this.listingDate = listingDate;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("wystaw", "moje", "anuluj").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(java.util.stream.Collectors.toList());
        }
        return new ArrayList<>();
    }
}
