package com.owntelecom.gui;

import com.owntelecom.OwnTelecomPlugin;
import com.owntelecom.database.model.Website;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class InternetBrowserGui implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final OwnTelecomPlugin plugin;
    private final Player player;
    private Inventory inventory;

    public InternetBrowserGui(OwnTelecomPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        List<Website> sites = plugin.getModuleManager().getInternetModule().listAvailableSites();
        if (sites.isEmpty()) {
            plugin.getMessageService().send(player, "internet.no-pages");
            return;
        }
        inventory = Bukkit.createInventory(null, Math.min(54, ((sites.size() / 9) + 1) * 9),
                LEGACY.deserialize("&8Internet"));
        for (Website site : sites) {
            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(LEGACY.deserialize("&b" + site.title()));
            meta.lore(List.of(
                    LEGACY.deserialize("&7/" + site.slug()),
                    LEGACY.deserialize("&eKliknij aby otworzyc")
            ));
            item.setItemMeta(meta);
            inventory.addItem(item);
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player clicker) || !clicker.equals(player)) {
            return;
        }
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta() || item.getItemMeta().lore() == null) {
            return;
        }
        String slugLine = LegacyComponentSerializer.legacySection().serialize(item.getItemMeta().lore().get(0));
        String slug = slugLine.replace("/", "").replace("§7", "").trim();
        plugin.getDatabaseManager().websites().findBySlug(slug).ifPresent(site -> {
            player.closeInventory();
            HandlerList.unregisterAll(this);
            plugin.getModuleManager().getInternetModule().loadPage(player, site, null);
        });
    }
}
