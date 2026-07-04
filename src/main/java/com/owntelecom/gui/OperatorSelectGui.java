package com.owntelecom.gui;

import com.owntelecom.OwnTelecomPlugin;
import com.owntelecom.database.model.Operator;
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

public class OperatorSelectGui implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final OwnTelecomPlugin plugin;
    private final Player player;
    private Inventory inventory;

    public OperatorSelectGui(OwnTelecomPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        List<Operator> operators = plugin.getDatabaseManager().operators().findAll();
        inventory = Bukkit.createInventory(null, Math.min(54, ((operators.size() / 9) + 1) * 9),
                LEGACY.deserialize("&8Wybierz operatora"));
        for (Operator op : operators) {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(LEGACY.deserialize("&b" + op.displayName()));
            meta.lore(List.of(
                    LEGACY.deserialize("&7ID: &f" + op.id()),
                    LEGACY.deserialize("&7/min: &f" + op.prepaidMinute()),
                    LEGACY.deserialize("&eKliknij aby dolaczyc")
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
        String idLine = LegacyComponentSerializer.legacySection().serialize(item.getItemMeta().lore().get(0));
        String id = idLine.replace("ID: ", "").replace("§7", "").replace("§f", "").trim();
        plugin.getDatabaseManager().subscribers().setOperator(player.getUniqueId(), id);
        plugin.getModuleManager().getCoverageService().invalidate(player.getUniqueId());
        player.sendMessage(plugin.getMessageService().colorize("&aDolaczono do operatora &f" + id));
        player.closeInventory();
        HandlerList.unregisterAll(this);
    }
}
