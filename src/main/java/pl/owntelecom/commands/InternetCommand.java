package pl.owntelecom.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.owntelecom.OwnTelecom;
import pl.owntelecom.models.Website;
import pl.owntelecom.models.ServerRoom;

import java.util.*;
import java.util.stream.Collectors;

public class InternetCommand implements CommandExecutor, TabCompleter {

    private final OwnTelecom plugin;

    public InternetCommand(OwnTelecom plugin) {
        this.plugin = plugin;
        plugin.getCommand("internet").setExecutor(this);
        plugin.getCommand("internet").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Ta komenda jest tylko dla graczy!");
            return true;
        }

        Player player = (Player) sender;

        // Sprawdź dostęp do internetu dla komend wymagających sieci
        if (args.length > 0 && !args[0].equalsIgnoreCase("help")) {
            if (!plugin.getInternetManager().hasInternetAccess(player)) {
                player.sendMessage(ChatColor.RED + "📡 Brak dostępu do internetu! Sprawdź zasięg sieci.");
                player.sendMessage(ChatColor.GRAY + "Potrzebujesz stacji z technologią LTE lub 5G.");
                return true;
            }
        }

        if (args.length == 0) {
            showMainMenu(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            // ============ PRZEGLĄDANIE ============
            case "otworz":
            case "open":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użycie: /internet otworz <ID_Strony>");
                    return true;
                }
                plugin.getInternetManager().openWebsite(player, args[1]);
                break;

            case "szukaj":
            case "search":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użycie: /internet szukaj <nazwa>");
                    return true;
                }
                handleSearch(player, args[1]);
                break;

            case "lista":
            case "list":
                handleList(player, args.length > 1 ? args[1] : null);
                break;

            // ============ SERWEROWNIE ============
            case "serwerownia":
                if (args.length < 2) {
                    sendServerRoomHelp(player);
                    return true;
                }
                handleServerRoom(player, args);
                break;

            // ============ ZARZĄDZANIE STRONAMI ============
            case "utworz":
            case "create":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Użycie: /internet utworz <nazwa> <ID_Serwerowni> [typ]");
                    player.sendMessage(ChatColor.GRAY + "Typy: NORMAL, SHOP, SOCIAL");
                    return true;
                }
                String type = args.length > 3 ? args[3] : "NORMAL";
                plugin.getInternetManager().createWebsite(player, args[1], args[2], type);
                break;

            case "edytuj":
            case "edit":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użycie: /internet edytuj <ID_Strony>");
                    return true;
                }
                handleEdit(player, args[1]);
                break;

            case "zapisz":
            case "save":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użycie: /internet zapisz <ID_Strony>");
                    return true;
                }
                plugin.getInternetManager().saveWebsiteFromBook(player, args[1]);
                break;

            case "wylacz":
            case "disable":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użycie: /internet wylacz <ID_Strony>");
                    return true;
                }
                handleToggleSite(player, args[1], false);
                break;

            case "wlacz":
            case "enable":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użycie: /internet wlacz <ID_Strony>");
                    return true;
                }
                handleToggleSite(player, args[1], true);
                break;

            case "usun":
            case "delete":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użycie: /internet usun <ID_Strony>");
                    return true;
                }
                handleDeleteSite(player, args[1]);
                break;

            // ============ SKLEP ============
            case "sklep":
            case "shop":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użycie: /internet sklep <ID_Sklepu>");
                    return true;
                }
                plugin.getInternetManager().openWebsite(player, args[1]);
                break;

            case "dodaj":
            case "additem":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Użycie: /internet dodaj <ID_Sklepu> <cena>");
                    player.sendMessage(ChatColor.GRAY + "Trzymaj przedmiot w ręce!");
                    return true;
                }
                try {
                    double price = Double.parseDouble(args[2]);
                    plugin.getInternetManager().addShopItem(player, args[1], price);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Nieprawidłowa cena!");
                }
                break;

            case "kup":
            case "buy":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Użycie: /internet kup <ID_Sklepu> <numer_przedmiotu>");
                    return true;
                }
                try {
                    int itemNum = Integer.parseInt(args[2]);
                    plugin.getInternetManager().buyShopItem(player, args[1], itemNum);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Nieprawidłowy numer przedmiotu!");
                }
                break;

            // ============ SOCIAL MEDIA ============
            case "tweet":
            case "twitter":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użycie: /internet tweet <treść>");
                    return true;
                }
                StringBuilder tweet = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    if (i > 1) tweet.append(" ");
                    tweet.append(args[i]);
                }
                plugin.getInternetManager().sendTweet(player, tweet.toString());
                break;

            // ============ INFORMACJE ============
            case "info":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użycie: /internet info <ID_Strony/Serwerowni>");
                    return true;
                }
                handleInfo(player, args[1]);
                break;

            case "speedtest":
            case "predkosc":
                handleSpeedTest(player);
                break;

            case "mojestrony":
            case "mysites":
                handleMySites(player);
                break;

            default:
                showMainMenu(player);
                break;
        }

        return true;
    }

    // ==================== MENU GŁÓWNE ====================
    private void showMainMenu(Player player) {
        player.sendMessage(ChatColor.GOLD + "══════ 🌐 Internet - Menu Główne ══════");
        player.sendMessage(ChatColor.YELLOW + "/internet lista [normal/shop/social] " + ChatColor.WHITE + "- Lista stron");
        player.sendMessage(ChatColor.YELLOW + "/internet szukaj <nazwa> " + ChatColor.WHITE + "- Szukaj strony");
        player.sendMessage(ChatColor.YELLOW + "/internet otworz <id> " + ChatColor.WHITE + "- Otwórz stronę");
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "📡 Serwerownie:");
        player.sendMessage(ChatColor.YELLOW + "/internet serwerownia utworz " + ChatColor.WHITE + "- Postaw serwerownię");
        player.sendMessage(ChatColor.YELLOW + "/internet serwerownia ulepsz <id> " + ChatColor.WHITE + "- Ulepsz");
        player.sendMessage(ChatColor.YELLOW + "/internet serwerownia lista " + ChatColor.WHITE + "- Lista");
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "📄 Strony:");
        player.sendMessage(ChatColor.YELLOW + "/internet utworz <nazwa> <serwerownia> [typ] " + ChatColor.WHITE + "- Nowa strona");
        player.sendMessage(ChatColor.YELLOW + "/internet edytuj <id> " + ChatColor.WHITE + "- Edytuj stronę (książka)");
        player.sendMessage(ChatColor.YELLOW + "/internet zapisz <id> " + ChatColor.WHITE + "- Zapisz z książki");
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "🛒 Sklepy:");
        player.sendMessage(ChatColor.YELLOW + "/internet dodaj <sklep> <cena> " + ChatColor.WHITE + "- Dodaj przedmiot");
        player.sendMessage(ChatColor.YELLOW + "/internet kup <sklep> <nr> " + ChatColor.WHITE + "- Kup przedmiot");
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "🐦 Social Media:");
        player.sendMessage(ChatColor.YELLOW + "/internet tweet <treść> " + ChatColor.WHITE + "- Wyślij tweeta ($50)");
        player.sendMessage(ChatColor.GRAY + "  (Działa jak czat globalny dla osób z internetem)");
        player.sendMessage("");
        player.sendMessage(ChatColor.AQUA + "/internet speedtest " + ChatColor.WHITE + "- Sprawdź prędkość");
        player.sendMessage(ChatColor.AQUA + "/internet mojestrony " + ChatColor.WHITE + "- Twoje strony");
    }

    // ==================== SERWEROWNIE ====================
    private void handleServerRoom(Player player, String[] args) {
        switch (args[1].toLowerCase()) {
            case "utworz":
            case "create":
                plugin.getInternetManager().createServerRoom(player);
                break;

            case "ulepsz":
            case "upgrade":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Użycie: /internet serwerownia ulepsz <ID>");
                    return;
                }
                plugin.getInternetManager().upgradeServerRoom(player, args[2]);
                break;

            case "lista":
            case "list":
                player.sendMessage(ChatColor.GOLD + "=== Serwerownie ===");
                for (ServerRoom room : plugin.getInternetManager().getAllServerRooms()) {
                    if (room.isActive()) {
                        player.sendMessage(ChatColor.YELLOW + "• " + room.getId() + 
                            ChatColor.GRAY + " [Lvl." + room.getLevel() + "] Sloty: " + 
                            room.getUsedSlots() + "/" + room.getMaxSlots());
                    }
                }
                break;

            case "info":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Użycie: /internet serwerownia info <ID>");
                    return;
                }
                ServerRoom room = plugin.getInternetManager().getServerRoom(args[2]);
                if (room != null) {
                    player.sendMessage(ChatColor.GOLD + "=== Serwerownia: " + room.getId() + " ===");
                    player.sendMessage(ChatColor.YELLOW + "Właściciel: " + 
                        ChatColor.WHITE + plugin.getServer().getOfflinePlayer(room.getOwner()).getName());
                    player.sendMessage(ChatColor.YELLOW + "Poziom: " + ChatColor.WHITE + room.getLevel() + "/3");
                    player.sendMessage(ChatColor.YELLOW + "Sloty: " + ChatColor.WHITE + 
                        room.getUsedSlots() + "/" + room.getMaxSlots());
                    player.sendMessage(ChatColor.YELLOW + "Maks. odwiedzin: " + ChatColor.WHITE + room.getMaxVisitors());
                    player.sendMessage(ChatColor.YELLOW + "Hostowane strony: " + 
                        ChatColor.WHITE + room.getHostedSites().size());
                }
                break;

            default:
                sendServerRoomHelp(player);
                break;
        }
    }

    private void sendServerRoomHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Serwerownia - Pomoc ===");
        player.sendMessage(ChatColor.YELLOW + "/internet serwerownia utworz " + ChatColor.WHITE + "- Postaw serwerownię");
        player.sendMessage(ChatColor.YELLOW + "/internet serwerownia ulepsz <ID> " + ChatColor.WHITE + "- Ulepsz serwerownię");
        player.sendMessage(ChatColor.YELLOW + "/internet serwerownia lista " + ChatColor.WHITE + "- Lista serwerowni");
        player.sendMessage(ChatColor.YELLOW + "/internet serwerownia info <ID> " + ChatColor.WHITE + "- Info");
    }

    // ==================== POZOSTAŁE HANDLERY ====================
    
    private void handleSearch(Player player, String query) {
        player.sendMessage(ChatColor.GOLD + "=== Wyniki wyszukiwania: " + query + " ===");
        boolean found = false;
        
        for (Website site : plugin.getInternetManager().getAllWebsites()) {
            if (site.isActive() && 
                (site.getName().toLowerCase().contains(query.toLowerCase()) ||
                 site.getId().toLowerCase().contains(query.toLowerCase()) ||
                 site.getDescription().toLowerCase().contains(query.toLowerCase()))) {
                player.sendMessage(ChatColor.YELLOW + "• " + site.getName() + 
                    ChatColor.GRAY + " [" + site.getType() + "] ID: " + site.getId());
                found = true;
            }
        }
        
        if (!found) {
            player.sendMessage(ChatColor.GRAY + "Brak wyników.");
        }
    }

    private void handleList(Player player, String typeFilter) {
        Collection<Website> sites;
        
        if (typeFilter != null) {
            try {
                Website.SiteType type = Website.SiteType.valueOf(typeFilter.toUpperCase());
                sites = plugin.getInternetManager().getWebsitesByType(type);
                player.sendMessage(ChatColor.GOLD + "=== Strony (" + type + ") ===");
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Nieprawidłowy typ! Dostępne: NORMAL, SHOP, SOCIAL");
                return;
            }
        } else {
            sites = plugin.getInternetManager().getAllWebsites();
            player.sendMessage(ChatColor.GOLD + "=== Wszystkie strony ===");
        }

        for (Website site : sites) {
            if (site.isActive()) {
                String typeIcon = site.getType() == Website.SiteType.SHOP ? "🛒" :
                                 site.getType() == Website.SiteType.SOCIAL ? "🐦" : "📄";
                player.sendMessage(typeIcon + " " + ChatColor.YELLOW + site.getName() + 
                    ChatColor.GRAY + " (ID: " + site.getId() + ") " + 
                    ChatColor.WHITE + site.getDescription());
            }
        }
    }

    private void handleEdit(Player player, String siteId) {
        Website site = plugin.getInternetManager().getWebsite(siteId);
        if (site == null) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono strony!");
            return;
        }
        
        if (!site.getOwner().equals(player.getUniqueId()) && !player.hasPermission("owntelecom.admin")) {
            player.sendMessage(ChatColor.RED + "Nie jesteś właścicielem tej strony!");
            return;
        }

        // Daj książkę do edycji
        ItemStack book = new ItemStack(Material.WRITABLE_BOOK);
        org.bukkit.inventory.meta.BookMeta meta = (org.bukkit.inventory.meta.BookMeta) book.getItemMeta();
        
        if (meta != null) {
            meta.setTitle("Edycja: " + site.getName());
            meta.setAuthor(player.getName());
            
            for (String page : site.getPages()) {
                meta.addPage(ChatColor.translateAlternateColorCodes('&', page));
            }
            
            if (site.getPages().isEmpty()) {
                meta.addPage("Witaj na stronie " + site.getName() + "!\n\nEdytuj tę książkę...");
            }
            
            book.setItemMeta(meta);
        }
        
        player.getInventory().addItem(book);
        player.sendMessage(ChatColor.GREEN + "📖 Otrzymałeś książkę do edycji!");
        player.sendMessage(ChatColor.YELLOW + "Po edycji użyj: /internet zapisz " + siteId);
    }

    private void handleToggleSite(Player player, String siteId, boolean enable) {
        Website site = plugin.getInternetManager().getWebsite(siteId);
        if (site == null) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono strony!");
            return;
        }

        if (!site.getOwner().equals(player.getUniqueId()) && !player.hasPermission("owntelecom.admin")) {
            player.sendMessage(ChatColor.RED + "Nie masz uprawnień!");
            return;
        }

        site.setActive(enable);
        plugin.getInternetManager().saveWebsites();
        
        player.sendMessage(enable ? 
            ChatColor.GREEN + "✅ Strona włączona!" : 
            ChatColor.RED + "⛔ Strona wyłączona!");
    }

    private void handleDeleteSite(Player player, String siteId) {
        Website site = plugin.getInternetManager().getWebsite(siteId);
        if (site == null) {
            player.sendMessage(ChatColor.RED + "Nie znaleziono strony!");
            return;
        }

        if (!site.getOwner().equals(player.getUniqueId()) && !player.hasPermission("owntelecom.admin")) {
            player.sendMessage(ChatColor.RED + "Nie masz uprawnień!");
            return;
        }

        // Usuń z serwerowni
        ServerRoom room = plugin.getInternetManager().getServerRoom(site.getServerRoomId());
        if (room != null) {
            room.removeSite(siteId);
        }

        // Usuń stronę
        plugin.getInternetManager().getAllWebsites().remove(site);
        plugin.getInternetManager().saveAll();
        
        player.sendMessage(ChatColor.RED + "🗑️ Usunięto stronę: " + site.getName());
    }

    private void handleInfo(Player player, String id) {
        // Sprawdź czy to strona czy serwerownia
        Website site = plugin.getInternetManager().getWebsite(id);
        if (site != null) {
            player.sendMessage(ChatColor.GOLD + "=== Strona: " + site.getName() + " ===");
            player.sendMessage(ChatColor.YELLOW + "ID: " + ChatColor.WHITE + site.getId());
            player.sendMessage(ChatColor.YELLOW + "Typ: " + ChatColor.WHITE + site.getType());
            player.sendMessage(ChatColor.YELLOW + "Właściciel: " + ChatColor.WHITE + 
                plugin.getServer().getOfflinePlayer(site.getOwner()).getName());
            player.sendMessage(ChatColor.YELLOW + "Serwerownia: " + ChatColor.WHITE + site.getServerRoomId());
            player.sendMessage(ChatColor.YELLOW + "Aktywna: " + (site.isActive() ? 
                ChatColor.GREEN + "Tak" : ChatColor.RED + "Nie"));
            player.sendMessage(ChatColor.YELLOW + "Odwiedzin: " + ChatColor.WHITE + site.getVisitCount());
            
            if (site.getType() == Website.SiteType.SHOP) {
                player.sendMessage(ChatColor.YELLOW + "Przedmiotów: " + 
                    ChatColor.WHITE + site.getShopItems().size());
            }
            return;
        }

        ServerRoom room = plugin.getInternetManager().getServerRoom(id);
        if (room != null) {
            player.sendMessage(ChatColor.GOLD + "=== Serwerownia: " + room.getId() + " ===");
            player.sendMessage(ChatColor.YELLOW + "Właściciel: " + ChatColor.WHITE + 
                plugin.getServer().getOfflinePlayer(room.getOwner()).getName());
            player.sendMessage(ChatColor.YELLOW + "Poziom: " + ChatColor.WHITE + room.getLevel() + "/3");
            player.sendMessage(ChatColor.YELLOW + "Sloty: " + ChatColor.WHITE + 
                room.getUsedSlots() + "/" + room.getMaxSlots());
            return;
        }

        player.sendMessage(ChatColor.RED + "Nie znaleziono strony ani serwerowni: " + id);
    }

    private void handleSpeedTest(Player player) {
        player.sendMessage(ChatColor.AQUA + "⏳ Wykonywanie testu prędkości...");
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            double speed = plugin.getInternetManager().getPlayerInternetSpeed(player);
            String speedText;
            
            if (speed <= 0) {
                speedText = ChatColor.RED + "Brak połączenia";
            } else if (speed < 1) {
                speedText = ChatColor.RED + String.format("%.1f Mb/s (Bardzo wolno)", speed);
            } else if (speed < 10) {
                speedText = ChatColor.GOLD + String.format("%.1f Mb/s (Wolno)", speed);
            } else if (speed < 50) {
                speedText = ChatColor.YELLOW + String.format("%.1f Mb/s (Średnio)", speed);
            } else if (speed < 100) {
                speedText = ChatColor.GREEN + String.format("%.1f Mb/s (Szybko)", speed);
            } else {
                speedText = ChatColor.DARK_GREEN + String.format("%.1f Mb/s (Bardzo szybko!)", speed);
            }
            
            player.sendMessage(ChatColor.GOLD + "══════ Wynik SpeedTest ══════");
            player.sendMessage(ChatColor.YELLOW + "Prędkość: " + speedText);
            player.sendMessage(ChatColor.GRAY + "Dostęp do internetu: " + 
                (plugin.getInternetManager().hasInternetAccess(player) ? 
                ChatColor.GREEN + "✓" : ChatColor.RED + "✗"));
        }, 40L); // 2 sekundy opóźnienia
    }

    private void handleMySites(Player player) {
        List<Website> mySites = plugin.getInternetManager().getPlayerWebsites(player.getUniqueId());
        
        if (mySites.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Nie posiadasz żadnych stron.");
            player.sendMessage(ChatColor.GRAY + "Utwórz: /internet utworz <nazwa> <ID_Serwerowni>");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== Twoje strony (" + mySites.size() + ") ===");
        for (Website site : mySites) {
            String status = site.isActive() ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗";
            String typeIcon = site.getType() == Website.SiteType.SHOP ? "🛒" :
                             site.getType() == Website.SiteType.SOCIAL ? "🐦" : "📄";
            player.sendMessage(typeIcon + " " + status + " " + ChatColor.YELLOW + site.getName() + 
                ChatColor.GRAY + " (ID: " + site.getId() + ") " + 
                ChatColor.WHITE + "Odwiedzin: " + site.getVisitCount());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList(
                "otworz", "szukaj", "lista", "utworz", "edytuj", "zapisz",
                "serwerownia", "sklep", "dodaj", "kup", "tweet", "info",
                "speedtest", "mojestrony", "wylacz", "wlacz", "usun"
            ));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "otworz":
                case "open":
                case "edytuj":
                case "edit":
                case "zapisz":
                case "save":
                case "info":
                case "wylacz":
                case "wlacz":
                case "usun":
                    completions.addAll(plugin.getInternetManager().getAllWebsites().stream()
                        .map(Website::getId)
                        .collect(Collectors.toList()));
                    break;
                case "sklep":
                case "shop":
                case "dodaj":
                case "additem":
                case "kup":
                case "buy":
                    completions.addAll(plugin.getInternetManager().getWebsitesByType(Website.SiteType.SHOP).stream()
                        .map(Website::getId)
                        .collect(Collectors.toList()));
                    break;
                case "lista":
                    completions.addAll(Arrays.asList("NORMAL", "SHOP", "SOCIAL"));
                    break;
                case "serwerownia":
                    completions.addAll(Arrays.asList("utworz", "ulepsz", "lista", "info"));
                    break;
                case "utworz":
                case "create":
                    completions.addAll(plugin.getInternetManager().getAllServerRooms().stream()
                        .map(ServerRoom::getId)
                        .collect(Collectors.toList()));
                    break;
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("serwerownia")) {
            switch (args[1].toLowerCase()) {
                case "ulepsz":
                case "info":
                    completions.addAll(plugin.getInternetManager().getAllServerRooms().stream()
                        .map(ServerRoom::getId)
                        .collect(Collectors.toList()));
                    break;
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("utworz")) {
            completions.addAll(Arrays.asList("NORMAL", "SHOP", "SOCIAL"));
        }

        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
}
