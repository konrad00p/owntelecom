package com.owntelecom.gui;

import com.owntelecom.OwnTelecomPlugin;
import com.owntelecom.database.repository.PackageRepository;
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
import java.util.Map;

public class PackageSelectGui implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final OwnTelecomPlugin plugin;
    private final Player player;
    private final String operatorId;
    private Inventory inventory;

    public PackageSelectGui(OwnTelecomPlugin plugin, Player player, String operatorId) {
        this.plugin = plugin;
        this.player = player;
        this.operatorId = operatorId;
    }

    public void open() {
        List<PackageRepository.ServicePackage> packages =
                plugin.getDatabaseManager().packages().findByOperator(operatorId);
        if (packages.isEmpty()) {
            player.sendMessage("Brak pakietow.");
            return;
        }
        inventory = Bukkit.createInventory(null, Math.min(54, ((packages.size() / 9) + 1) * 9),
                LEGACY.deserialize("&8Pakiety"));
        for (PackageRepository.ServicePackage pkg : packages) {
            ItemStack item = new ItemStack(Material.CHEST);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(LEGACY.deserialize("&b" + pkg.name()));
            meta.lore(List.of(
                    LEGACY.deserialize("&7Cena: &f" + pkg.price()),
                    LEGACY.deserialize("&7" + pkg.minutes() + " min | " + pkg.sms() + " SMS | " + pkg.mb() + " MB"),
                    LEGACY.deserialize("&7Czas: &f" + pkg.durationDays() + " dni"),
                    LEGACY.deserialize("&eKliknij aby kupic")
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
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        String name = LegacyComponentSerializer.legacySection().serialize(item.getItemMeta().displayName())
                .replace("§b", "");
        plugin.getDatabaseManager().packages().findByOperator(operatorId).stream()
                .filter(p -> p.name().equals(name))
                .findFirst()
                .ifPresent(pkg -> {
                    if (!plugin.getEconomyService().withdraw(player, pkg.price())) {
                        plugin.getMessageService().send(player, "errors.insufficient-funds",
                                Map.of("cost", String.valueOf(pkg.price())));
                        return;
                    }
                    long expires = System.currentTimeMillis() + pkg.durationDays() * 86400000L;
                    var sub = plugin.getDatabaseManager().subscribers().findByPlayer(player.getUniqueId());
                    sub.ifPresent(s -> {
                        var updated = new com.owntelecom.database.model.Subscriber(
                                s.playerUuid(), s.operatorId(), pkg.id(),
                                pkg.minutes(), pkg.sms(), pkg.mb(), expires
                        );
                        plugin.getDatabaseManager().subscribers().updatePackage(updated);
                        player.sendMessage(plugin.getMessageService().colorize("&aPakiet &f" + pkg.name() + " &aktywowany."));
                    });
                    player.closeInventory();
                    HandlerList.unregisterAll(this);
                });
    }
}
