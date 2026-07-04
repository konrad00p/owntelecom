package com.owntelecom.gui;

import com.owntelecom.OwnTelecomPlugin;
import com.owntelecom.database.model.Operator;
import com.owntelecom.database.model.Station;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StationCreateGui implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final OwnTelecomPlugin plugin;
    private final Player player;
    private final Operator operator;
    private Inventory inventory;

    public StationCreateGui(OwnTelecomPlugin plugin, Player player, Operator operator) {
        this.plugin = plugin;
        this.player = player;
        this.operator = operator;
    }

    public void open() {
        ConfigurationSection techs = plugin.getConfigManager().getTechnologies().getConfigurationSection("technologies");
        if (techs == null) {
            player.sendMessage("Brak skonfigurowanych technologii.");
            return;
        }
        inventory = Bukkit.createInventory(null, 27,
                LEGACY.deserialize("&8Stacja - " + operator.displayName()));
        for (String key : techs.getKeys(false)) {
            ConfigurationSection tech = techs.getConfigurationSection(key);
            if (tech == null) {
                continue;
            }
            Material mat = Material.matchMaterial(tech.getString("block", "IRON_BLOCK"));
            if (mat == null) {
                mat = Material.IRON_BLOCK;
            }
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(LEGACY.deserialize(tech.getString("display-name", key)));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(LEGACY.deserialize("&7Koszt: &f" + tech.getDouble("create-cost", 0)));
            lore.add(LEGACY.deserialize("&7Zasieg: &f" + tech.getDouble("base-range", 0)));
            lore.add(LEGACY.deserialize("&7Predkosc: &f" + tech.getDouble("base-speed-mbps", 0) + " Mb/s"));
            lore.add(LEGACY.deserialize("&7Awaria: &f" + (tech.getDouble("failure-chance-base", 0) * 100) + "%"));
            lore.add(LEGACY.deserialize("&7Blok: &f" + tech.getString("block")));
            lore.add(LEGACY.deserialize("&eKliknij aby zbudowac"));
            meta.lore(lore);
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
        String techId = findTechId(item.getType());
        if (techId == null) {
            return;
        }
        var created = plugin.getModuleManager().getStationModule().createStation(player, operator, techId);
        if (created.isPresent()) {
            Station st = created.get();
            plugin.getMessageService().send(player, "station.created", Map.of(
                    "tech", techId,
                    "level", String.valueOf(st.level())
            ));
        } else {
            ConfigurationSection tech = plugin.getModuleManager().getCoverageService().getTechnology(techId);
            plugin.getMessageService().send(player, "station.wrong-block", Map.of(
                    "block", tech != null ? tech.getString("block", "?") : "?"
            ));
        }
        player.closeInventory();
        HandlerList.unregisterAll(this);
    }

    private String findTechId(Material material) {
        ConfigurationSection techs = plugin.getConfigManager().getTechnologies().getConfigurationSection("technologies");
        if (techs == null) {
            return null;
        }
        for (String key : techs.getKeys(false)) {
            Material mat = Material.matchMaterial(techs.getString(key + ".block", ""));
            if (material.equals(mat)) {
                return key;
            }
        }
        return null;
    }
}
