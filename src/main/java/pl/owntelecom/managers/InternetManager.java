package pl.owntelecom.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.owntelecom.OwnTelecom;
import pl.owntelecom.models.ServerRoom;
import pl.owntelecom.models.Website;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InternetManager {

    private final OwnTelecom plugin;
    private final Map<String, ServerRoom> serverRooms;
    private final Map<String, Website> websites;
    private final File serverRoomsFile;
    private final File websitesFile;

    public InternetManager(OwnTelecom plugin) {
        this.plugin = plugin;
        this.serverRooms = new ConcurrentHashMap<>();
        this.websites = new ConcurrentHashMap<>();
        this.serverRoomsFile = new File(plugin.getDataFolder(), "serverrooms.yml");
        this.websitesFile = new File(plugin.getDataFolder(), "websites.yml");
        
        loadServerRooms();
        loadWebsites();
    }

    // ==================== SERWEROWNIE ====================

    public boolean createServerRoom(Player player) {
        // Sprawdź czy gracz patrzy na odpowiedni blok (np. IRON_BLOCK jako serwer)
        Location targetLocation = player.getTargetBlock(null, 10).getLocation();
        
        if (targetLocation.getBlock().getType() != Material.IRON_BLOCK) {
            player.sendMessage(ChatColor.RED + "Musisz patrzeć na BLOK ŻELAZA aby postawić serwerownię!");
            return false;
        }

        // Sprawdź czy w pobliżu nie ma już serwerowni
        for (ServerRoom room : serverRooms.values()) {
            if (room.getLocation().distance(targetLocation) < 5) {
                player.sendMessage(ChatColor.RED + "Zbyt blisko innej serwerowni! (min. 5 bloków)");
                return false;
            }
        }

        double cost = 5000.0;
        if (!plugin.getEconomy().has(player, cost)) {
            player.sendMessage(ChatColor.RED + "Potrzebujesz $" + cost + " na budowę serwerowni!");
            return false;
        }

        plugin.getEconomy().withdrawPlayer(player, cost);
        
        String roomId = "serverroom_" + player.getName().toLowerCase() + "_" + System.currentTimeMillis();
        ServerRoom room = new ServerRoom(roomId, player.getUniqueId(), targetLocation);
        serverRooms.put(roomId, room);
        
        saveServerRooms();
        
        player.sendMessage(ChatColor.GREEN + "✅ Postawiono serwerownię! ID: " + roomId);
        player.sendMessage(ChatColor.YELLOW + "Sloty: " + room.getMaxSlots() + " | Poziom: " + room.getLevel());
        return true;
    }

    public boolean upgradeServerRoom(Player player, String roomId) {
        ServerRoom room = serverRooms.get(roomId);
        if (room == null) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono serwerowni!");
            return false;
        }

        if (!room.getOwner().equals(player.getUniqueId()) && !player.hasPermission("owntelecom.admin")) {
            player.sendMessage(ChatColor.RED + "Nie jesteś właścicielem tej serwerowni!");
            return false;
        }

        if (room.getLevel() >= 3) {
            player.sendMessage(ChatColor.RED + "Serwerownia ma maksymalny poziom!");
            return false;
        }

        double cost = room.getUpgradeCost();
        if (!plugin.getEconomy().has(player, cost)) {
            player.sendMessage(ChatColor.RED + "Potrzebujesz $" + cost + " na ulepszenie!");
            return false;
        }

        plugin.getEconomy().withdrawPlayer(player, cost);
        room.setLevel(room.getLevel() + 1);
        saveServerRooms();
        
        player.sendMessage(ChatColor.GREEN + "✅ Ulepszono serwerownię do poziomu " + room.getLevel());
        player.sendMessage(ChatColor.YELLOW + "Sloty: " + room.getMaxSlots() + " | Odwiedziny: " + room.getMaxVisitors());
        return true;
    }

    // ==================== STRONY INTERNETOWE ====================

    public boolean createWebsite(Player player, String name, String serverRoomId, String typeStr) {
        // Sprawdź serwerownię
        ServerRoom room = serverRooms.get(serverRoomId);
        if (room == null) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono serwerowni!");
            return false;
        }

        if (!room.isActive()) {
            player.sendMessage(ChatColor.RED + "Ta serwerownia jest nieaktywna!");
            return false;
        }

        if (room.getFreeSlots() <= 0) {
            player.sendMessage(ChatColor.RED + "Brak wolnych slotów w tej serwerowni!");
            return false;
        }

        // Ustal typ strony
        Website.SiteType type;
        try {
            type = Website.SiteType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Nieprawidłowy typ! Dostępne: NORMAL, SHOP, SOCIAL");
            return false;
        }

        double cost = type == Website.SiteType.SHOP ? 2000.0 : 500.0;
        if (!plugin.getEconomy().has(player, cost)) {
            player.sendMessage(ChatColor.RED + "Potrzebujesz $" + cost + " na utworzenie strony!");
            return false;
        }

        plugin.getEconomy().withdrawPlayer(player, cost);
        
        String siteId = player.getName().toLowerCase() + "_" + name.toLowerCase().replace(" ", "_");
        Website website = new Website(siteId, name, player.getUniqueId(), serverRoomId, type);
        
        websites.put(siteId, website);
        room.addSite(siteId);
        
        saveWebsites();
        saveServerRooms();
        
        player.sendMessage(ChatColor.GREEN + "✅ Stworzono stronę: " + name);
        player.sendMessage(ChatColor.YELLOW + "ID: " + siteId + " | Typ: " + type);
        
        // Daj graczowi książkę do edycji
        if (type == Website.SiteType.NORMAL) {
            giveEditBook(player, website);
        }
        
        return true;
    }

    // Daj książkę do edycji strony
    private void giveEditBook(Player player, Website website) {
        ItemStack book = new ItemStack(Material.WRITABLE_BOOK);
        org.bukkit.inventory.meta.BookMeta meta = (org.bukkit.inventory.meta.BookMeta) book.getItemMeta();
        
        if (meta != null) {
            meta.setTitle(ChatColor.GOLD + "Edycja: " + website.getName());
            meta.setAuthor(player.getName());
            
            // Wczytaj istniejące strony
            for (String page : website.getPages()) {
                meta.addPage(ChatColor.translateAlternateColorCodes('&', page));
            }
            
            // Jeśli pusta, dodaj stronę startową
            if (website.getPages().isEmpty()) {
                meta.addPage(ChatColor.GREEN + "Witaj na stronie " + website.getName() + "!\n\n" +
                            ChatColor.BLACK + "Edytuj tę książkę i użyj /internet zapisz " + website.getId());
            }
            
            book.setItemMeta(meta);
        }
        
        player.getInventory().addItem(book);
        player.sendMessage(ChatColor.GREEN + "📖 Otrzymałeś książkę do edycji strony!");
        player.sendMessage(ChatColor.YELLOW + "Po edycji użyj: /internet zapisz " + website.getId());
    }

    // Zapisz treść strony z książki
    public boolean saveWebsiteFromBook(Player player, String siteId) {
        Website website = websites.get(siteId);
        if (website == null) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono strony!");
            return false;
        }

        if (!website.getOwner().equals(player.getUniqueId()) && !player.hasPermission("owntelecom.admin")) {
            player.sendMessage(ChatColor.RED + "Nie jesteś właścicielem tej strony!");
            return false;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() != Material.WRITABLE_BOOK && heldItem.getType() != Material.WRITTEN_BOOK) {
            player.sendMessage(ChatColor.RED + "Musisz trzymać książkę w ręce!");
            return false;
        }

        if (!(heldItem.getItemMeta() instanceof org.bukkit.inventory.meta.BookMeta)) {
            player.sendMessage(ChatColor.RED + "To nie jest prawidłowa książka!");
            return false;
        }

        org.bukkit.inventory.meta.BookMeta meta = (org.bukkit.inventory.meta.BookMeta) heldItem.getItemMeta();
        
        // Zapisz strony
        website.getPages().clear();
        for (String page : meta.getPages()) {
            website.addPage(page);
        }
        website.setContent(String.join("\n---\n", meta.getPages()));
        
        saveWebsites();
        
        player.sendMessage(ChatColor.GREEN + "✅ Zapisano treść strony: " + website.getName());
        player.sendMessage(ChatColor.GRAY + "Liczba stron: " + website.getPages().size());
        
        return true;
    }

    // Otwórz stronę jako książkę
    public boolean openWebsite(Player player, String siteId) {
        Website website = websites.get(siteId);
        if (website == null) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono strony: " + siteId);
            return false;
        }

        if (!website.isActive()) {
            player.sendMessage(ChatColor.RED + "Ta strona jest obecnie wyłączona!");
            return false;
        }

        // Sprawdź czy gracz ma dostęp do internetu
        if (!hasInternetAccess(player)) {
            player.sendMessage(ChatColor.RED + "Brak dostępu do internetu! Sprawdź zasięg.");
            return false;
        }

        // Symulacja ładowania przy słabym internecie
        double speed = getPlayerInternetSpeed(player);
        
        if (speed < 1.0) {
            player.sendMessage(ChatColor.GRAY + "⏳ Wczytywanie strony... (Wolne łącze)");
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                displayWebsite(player, website);
            }, (long)(40 / Math.max(0.1, speed))); // Opóźnienie zależne od prędkości
        } else {
            displayWebsite(player, website);
        }

        website.incrementVisits();
        return true;
    }

    private void displayWebsite(Player player, Website website) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        org.bukkit.inventory.meta.BookMeta meta = (org.bukkit.inventory.meta.BookMeta) book.getItemMeta();
        
        if (meta != null) {
            meta.setTitle(ChatColor.GOLD + "🌐 " + website.getName());
            meta.setAuthor(plugin.getServer().getOfflinePlayer(website.getOwner()).getName());
            
            if (website.getType() == Website.SiteType.SHOP) {
                // Dla sklepu - generuj strony z przedmiotami
                generateShopPages(website, meta);
            } else {
                // Dla normalnej strony - pokaż zapisane strony
                for (String page : website.getPages()) {
                    meta.addPage(ChatColor.translateAlternateColorCodes('&', page));
                }
            }
            
            if (meta.getPages().isEmpty()) {
                meta.addPage(ChatColor.GRAY + "Ta strona jest pusta.");
            }
            
            book.setItemMeta(meta);
        }
        
        player.openBook(book);
        player.sendMessage(ChatColor.GREEN + "📖 Otworzono: " + website.getName());
    }

    private void generateShopPages(Website website, org.bukkit.inventory.meta.BookMeta meta) {
        StringBuilder page = new StringBuilder();
        page.append(ChatColor.GOLD + "" + ChatColor.BOLD + "🛒 " + website.getName() + "\n\n");
        page.append(ChatColor.GRAY + "Sklep online\n");
        page.append(ChatColor.GRAY + "Użyj /internet kup " + website.getId() + " <nr>\n\n");
        
        int itemNum = 1;
        int itemsOnPage = 0;
        
        for (Map.Entry<ItemStack, Double> entry : website.getShopItems().entrySet()) {
            if (itemsOnPage >= 5) {
                meta.addPage(page.toString());
                page = new StringBuilder();
                itemsOnPage = 0;
            }
            
            ItemStack item = entry.getKey();
            double price = entry.getValue();
            
            page.append(ChatColor.YELLOW + "#" + itemNum + " " + 
                       ChatColor.WHITE + item.getType().name() + 
                       " x" + item.getAmount() + "\n");
            page.append(ChatColor.GREEN + "   Cena: $" + String.format("%.2f", price) + "\n\n");
            
            itemNum++;
            itemsOnPage++;
        }
        
        if (itemsOnPage > 0) {
            meta.addPage(page.toString());
        }
        
        if (website.getShopItems().isEmpty()) {
            meta.addPage(ChatColor.GRAY + "Sklep jest pusty.\n\nWłaściciel jeszcze nie dodał przedmiotów.");
        }
    }

    // ==================== E-COMMERCE ====================

    // Dodaj przedmiot do sklepu
    public boolean addShopItem(Player player, String siteId, double price) {
        Website website = websites.get(siteId);
        if (website == null || website.getType() != Website.SiteType.SHOP) {
            player.sendMessage(ChatColor.RED + "To nie jest sklep!");
            return false;
        }

        if (!website.getOwner().equals(player.getUniqueId()) && !player.hasPermission("owntelecom.admin")) {
            player.sendMessage(ChatColor.RED + "Nie jesteś właścicielem tego sklepu!");
            return false;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Musisz trzymać przedmiot w ręce!");
            return false;
        }

        if (price <= 0) {
            player.sendMessage(ChatColor.RED + "Cena musi być większa od 0!");
            return false;
        }

        ItemStack itemToAdd = heldItem.clone();
        website.addShopItem(itemToAdd, price);
        saveWebsites();
        
        player.sendMessage(ChatColor.GREEN + "✅ Dodano przedmiot do sklepu!");
        player.sendMessage(ChatColor.YELLOW + itemToAdd.getType().name() + " x" + itemToAdd.getAmount() + 
                          " - $" + String.format("%.2f", price));
        
        return true;
    }

    // Kup przedmiot ze sklepu
    public boolean buyShopItem(Player player, String siteId, int itemNumber) {
        Website website = websites.get(siteId);
        if (website == null || website.getType() != Website.SiteType.SHOP) {
            player.sendMessage(ChatColor.RED + "To nie jest sklep!");
            return false;
        }

        List<Map.Entry<ItemStack, Double>> items = new ArrayList<>(website.getShopItems().entrySet());
        
        if (itemNumber < 1 || itemNumber > items.size()) {
            player.sendMessage(ChatColor.RED + "Nieprawidłowy numer przedmiotu! (1-" + items.size() + ")");
            return false;
        }

        Map.Entry<ItemStack, Double> selectedItem = items.get(itemNumber - 1);
        ItemStack item = selectedItem.getKey();
        double price = selectedItem.getValue();

        // Sprawdź czy gracz ma pieniądze
        if (!plugin.getEconomy().has(player, price)) {
            player.sendMessage(ChatColor.RED + "Nie masz wystarczających środków! Potrzebujesz: $" + price);
            return false;
        }

        // Sprawdź miejsce w ekwipunku
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(ChatColor.RED + "Nie masz miejsca w ekwipunku!");
            return false;
        }

        // Pobierz pieniądze
        plugin.getEconomy().withdrawPlayer(player, price);
        
        // Przekaż pieniądze właścicielowi sklepu
        org.bukkit.OfflinePlayer owner = plugin.getServer().getOfflinePlayer(website.getOwner());
        plugin.getEconomy().depositPlayer(owner, price);
        
        // Daj przedmiot
        player.getInventory().addItem(item.clone());
        
        // Usuń przedmiot ze sklepu
        website.removeShopItem(itemNumber - 1);
        saveWebsites();
        
        player.sendMessage(ChatColor.GREEN + "✅ Kupiono " + item.getType().name() + 
                          " x" + item.getAmount() + " za $" + String.format("%.2f", price));
        
        // Powiadom właściciela
        Player ownerOnline = plugin.getServer().getPlayer(website.getOwner());
        if (ownerOnline != null && ownerOnline.isOnline()) {
            ownerOnline.sendMessage(ChatColor.GREEN + "💰 " + player.getName() + 
                                   " kupił przedmiot z Twojego sklepu za $" + String.format("%.2f", price));
        }
        
        return true;
    }

    // ==================== SOCIAL MEDIA ====================

    public boolean sendTweet(Player player, String message) {
        if (!hasInternetAccess(player)) {
            player.sendMessage(ChatColor.RED + "Potrzebujesz dostępu do internetu aby tweetować!");
            return false;
        }

        double tweetCost = 50.0;
        if (!plugin.getEconomy().has(player, tweetCost)) {
            player.sendMessage(ChatColor.RED + "Tweet kosztuje $" + tweetCost + "!");
            return false;
        }

        plugin.getEconomy().withdrawPlayer(player, tweetCost);
        
        // Wyślij tweeta do wszystkich z internetem
        String tweet = ChatColor.AQUA + "[🐦 Twitter] " + ChatColor.YELLOW + player.getName() + 
                      ChatColor.WHITE + ": " + message;
        
        int recipients = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (hasInternetAccess(online)) {
                online.sendMessage(tweet);
                recipients++;
            }
        }
        
        player.sendMessage(ChatColor.GREEN + "✅ Tweet wysłany! Dotarł do " + recipients + " osób.");
        plugin.getLogger().info("[Twitter] " + player.getName() + ": " + message);
        
        return true;
    }

    // ==================== POMOCNICZE ====================

    public boolean hasInternetAccess(Player player) {
        // Sprawdź czy gracz ma operatora z internetem
        for (var op : plugin.getOperatorManager().getActiveOperators()) {
            var station = plugin.getStationManager().findBestInternetStation(player, op.getId());
            if (station != null && station.isActive() && !station.isBroken()) {
                return true;
            }
        }
        return false;
    }

    public double getPlayerInternetSpeed(Player player) {
        double maxSpeed = 0;
        for (var op : plugin.getOperatorManager().getActiveOperators()) {
            double speed = plugin.getStationManager().getPlayerInternetSpeed(player, op.getId());
            if (speed > maxSpeed) maxSpeed = speed;
        }
        return maxSpeed;
    }

    // Gettery
    public ServerRoom getServerRoom(String id) { return serverRooms.get(id); }
    public Website getWebsite(String id) { return websites.get(id); }
    public Collection<ServerRoom> getAllServerRooms() { return serverRooms.values(); }
    public Collection<Website> getAllWebsites() { return websites.values(); }
    
    public List<Website> getWebsitesByType(Website.SiteType type) {
        return websites.values().stream()
            .filter(w -> w.getType() == type && w.isActive())
            .collect(Collectors.toList());
    }

    public List<Website> getPlayerWebsites(UUID playerId) {
        return websites.values().stream()
            .filter(w -> w.getOwner().equals(playerId))
            .collect(Collectors.toList());
    }

    // Zapis/Odczyt
    public void saveServerRooms() {
        FileConfiguration config = new YamlConfiguration();
        for (ServerRoom room : serverRooms.values()) {
            String path = "rooms." + room.getId();
            config.set(path + ".owner", room.getOwner().toString());
            config.set(path + ".location", room.locationToString());
            config.set(path + ".level", room.getLevel());
            config.set(path + ".active", room.isActive());
            config.set(path + ".rentalPrice", room.getRentalPrice());
            config.set(path + ".hostedSites", room.getHostedSites());
        }
        try { config.save(serverRoomsFile); } catch (IOException e) {
            plugin.getLogger().severe("Nie można zapisać serverrooms.yml!");
        }
    }

    public void saveWebsites() {
        FileConfiguration config = new YamlConfiguration();
        for (Website site : websites.values()) {
            String path = "sites." + site.getId();
            config.set(path + ".name", site.getName());
            config.set(path + ".owner", site.getOwner().toString());
            config.set(path + ".serverRoomId", site.getServerRoomId());
            config.set(path + ".type", site.getType().name());
            config.set(path + ".active", site.isActive());
            config.set(path + ".description", site.getDescription());
            config.set(path + ".visitCount", site.getVisitCount());
            config.set(path + ".pages", site.getPages());
            
            // Zapisz przedmioty sklepu (tylko dla SHOP)
            if (site.getType() == Website.SiteType.SHOP) {
                List<Map<String, Object>> items = new ArrayList<>();
                for (Map.Entry<ItemStack, Double> entry : site.getShopItems().entrySet()) {
                    Map<String, Object> itemMap = new LinkedHashMap<>();
                    itemMap.put("item", entry.getKey().serialize());
                    itemMap.put("price", entry.getValue());
                    items.add(itemMap);
                }
                config.set(path + ".shopItems", items);
            }
        }
        try { config.save(websitesFile); } catch (IOException e) {
            plugin.getLogger().severe("Nie można zapisać websites.yml!");
        }
    }

    public void saveAll() {
        saveServerRooms();
        saveWebsites();
    }

    private void loadServerRooms() {
        if (!serverRoomsFile.exists()) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(serverRoomsFile);
        ConfigurationSection roomsSection = config.getConfigurationSection("rooms");
        if (roomsSection == null) return;
        
        for (String id : roomsSection.getKeys(false)) {
            String path = "rooms." + id;
            UUID owner = UUID.fromString(config.getString(path + ".owner"));
            Location loc = ServerRoom.stringToLocation(config.getString(path + ".location"));
            if (loc == null) continue;
            
            ServerRoom room = new ServerRoom(id, owner, loc);
            room.setLevel(config.getInt(path + ".level", 1));
            room.setActive(config.getBoolean(path + ".active", true));
            room.setRentalPrice(config.getDouble(path + ".rentalPrice", 100.0));
            
            List<String> sites = config.getStringList(path + ".hostedSites");
            for (String siteId : sites) {
                room.addSite(siteId);
            }
            
            serverRooms.put(id, room);
        }
    }

    private void loadWebsites() {
        if (!websitesFile.exists()) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(websitesFile);
        ConfigurationSection sitesSection = config.getConfigurationSection("sites");
        if (sitesSection == null) return;
        
        for (String id : sitesSection.getKeys(false)) {
            String path = "sites." + id;
            UUID owner = UUID.fromString(config.getString(path + ".owner"));
            String serverRoomId = config.getString(path + ".serverRoomId");
            Website.SiteType type = Website.SiteType.valueOf(config.getString(path + ".type", "NORMAL"));
            
            Website site = new Website(id, config.getString(path + ".name", id), owner, serverRoomId, type);
            site.setActive(config.getBoolean(path + ".active", true));
            site.setDescription(config.getString(path + ".description", ""));
            
            List<String> pages = config.getStringList(path + ".pages");
            for (String page : pages) {
                site.addPage(page);
            }
            
            // Wczytaj przedmioty sklepu
            if (type == Website.SiteType.SHOP) {
                List<Map<?, ?>> items = config.getMapList(path + ".shopItems");
                for (Map<?, ?> itemMap : items) {
                    try {
                        ItemStack item = ItemStack.deserialize((Map<String, Object>) itemMap.get("item"));
                        double price = ((Number) itemMap.get("price")).doubleValue();
                        site.addShopItem(item, price);
                    } catch (Exception ignored) {}
                }
            }
            
            websites.put(id, site);
        }
    }
}
