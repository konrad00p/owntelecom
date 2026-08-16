package pl.owntelecom.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pl.owntelecom.OwnTelecom;
import pl.owntelecom.models.Station;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StationManager {

    private final OwnTelecom plugin;
    private final Map<String, Station> stations; // ID stacji -> Stacja
    private final Map<String, List<Station>> operatorStations; // ID operatora -> Lista stacji
    private final File stationsFile;
    private final File technologiesFile;
    private FileConfiguration technologiesConfig;

    public StationManager(OwnTelecom plugin) {
        this.plugin = plugin;
        this.stations = new ConcurrentHashMap<>();
        this.operatorStations = new ConcurrentHashMap<>();
        this.stationsFile = new File(plugin.getDataFolder(), "stations.yml");

        // Wczytaj konfigurację technologii
        this.technologiesFile = new File(plugin.getDataFolder(), "technologies.yml");
        if (!technologiesFile.exists()) {
            plugin.saveResource("technologies.yml", false);
        }
        this.technologiesConfig = YamlConfiguration.loadConfiguration(technologiesFile);

        loadStations();
        startFailureCheckTask();
    }

    // ========== DYNAMICZNE POBIERANIE PARAMETRÓW TECHNOLOGII ==========

    /**
     * Bazowy zasięg technologii w blokach
     */
    public double getTechnologyRange(String technology) {
        return technologiesConfig.getDouble("technologies." + technology + ".zasieg_bazowy", 50.0);
    }

    /**
     * Bazowa prędkość technologii w Mb/s
     */
    public double getTechnologySpeed(String technology) {
        return technologiesConfig.getDouble("technologies." + technology + ".predkosc_mbs", 1.0);
    }

    /**
     * Czy technologia obsługuje internet
     */
    public boolean isInternetSupported(String technology) {
        return technologiesConfig.getBoolean("technologies." + technology + ".obsluguje_internet", false);
    }

    // Tworzenie stacji
    public boolean createStation(Player player, String operatorId, String technology) {
        if (plugin.getOperatorManager().getOperator(operatorId) == null) {
            player.sendMessage("§cOperator o ID '" + operatorId + "' nie istnieje!");
            return false;
        }

        if (!plugin.getOperatorManager().getOperator(operatorId).isOwner(player.getUniqueId())
                && !player.hasPermission("owntelecom.admin")) {
            player.sendMessage("§cNie jesteś właścicielem tego operatora!");
            return false;
        }

        if (!technologiesConfig.contains("technologies." + technology)) {
            player.sendMessage("§cNieznana technologia! Sprawdź technologie.yml.");
            return false;
        }

        Location targetLocation = player.getTargetBlock(null, 10).getLocation();
        Material requiredBlock = plugin.getConfigManager().getStationBlock();

        if (targetLocation.getBlock().getType() != requiredBlock) {
            player.sendMessage("§cMusisz patrzeć na blok " + requiredBlock.name() + " aby postawić stację!");
            return false;
        }

        double cost = technologiesConfig.getDouble("technologies." + technology + ".cena_budowy", 1000.0);
        if (!plugin.getEconomy().has(player, cost)) {
            player.sendMessage("§cNie masz wystarczających środków! Potrzebujesz: $" + cost);
            return false;
        }

        String stationId = operatorId + "_station_" + System.currentTimeMillis();
        plugin.getEconomy().withdrawPlayer(player, cost);

        // Przekazujemy this (StationManager) do stacji
        Station station = new Station(stationId, operatorId, targetLocation, technology,
                player.getUniqueId(), this);
        stations.put(stationId, station);
        operatorStations.computeIfAbsent(operatorId, k -> new ArrayList<>()).add(station);

        saveStations();

        player.sendMessage("§aPostawiłeś stację §e" + technology + " §adla operatora §e" + operatorId);
        player.sendMessage("§aKoszt: §e$" + cost + " §a| Zasięg: §e" + station.getBaseRange() + " §abloków");
        return true;
    }

    // Ulepszanie stacji
    public boolean upgradeStation(Player player, String stationId) {
        Station station = stations.get(stationId);
        if (station == null) {
            player.sendMessage("§cNie znaleziono stacji!");
            return false;
        }

        if (!plugin.getOperatorManager().getOperator(station.getOperatorId()).isOwner(player.getUniqueId())
                && !player.hasPermission("owntelecom.admin")) {
            player.sendMessage("§cNie jesteś właścicielem tej stacji!");
            return false;
        }

        if (station.getLevel() >= 3) {
            player.sendMessage("§cStacja ma już maksymalny poziom (3)!");
            return false;
        }

        double cost = station.getUpgradeCost();
        if (!plugin.getEconomy().has(player, cost)) {
            player.sendMessage("§cNie masz wystarczających środków! Potrzebujesz: $" + cost);
            return false;
        }

        plugin.getEconomy().withdrawPlayer(player, cost);
        station.setLevel(station.getLevel() + 1);
        saveStations();

        player.sendMessage("§aUlepszono stację do poziomu §e" + station.getLevel());
        player.sendMessage("§aNowy zasięg: §e" + station.getBaseRange() + " §abloków");
        player.sendMessage("§aSzansa na awarię: §e" + station.getDamageChance() + "%");
        return true;
    }

    // Naprawa stacji
    public boolean repairStation(Player player, String stationId) {
        Station station = stations.get(stationId);
        if (station == null) {
            player.sendMessage("§cNie znaleziono stacji!");
            return false;
        }

        if (!station.isBroken()) {
            player.sendMessage("§cTa stacja nie jest uszkodzona!");
            return false;
        }

        double cost = station.getRepairCost();
        if (!plugin.getEconomy().has(player, cost)) {
            player.sendMessage("§cNie masz wystarczających środków! Potrzebujesz: $" + cost);
            return false;
        }

        plugin.getEconomy().withdrawPlayer(player, cost);
        station.setBroken(false);
        station.setActive(true);
        saveStations();

        player.sendMessage("§aNaprawiono stację! Koszt: §e$" + cost);
        return true;
    }

    // Usuwanie stacji
    public boolean removeStation(Player player, String stationId) {
        Station station = stations.get(stationId);
        if (station == null) {
            player.sendMessage("§cNie znaleziono stacji!");
            return false;
        }

        if (!plugin.getOperatorManager().getOperator(station.getOperatorId()).isOwner(player.getUniqueId())
                && !player.hasPermission("owntelecom.admin")) {
            player.sendMessage("§cNie jesteś właścicielem tej stacji!");
            return false;
        }

        stations.remove(stationId);
        List<Station> opStations = operatorStations.get(station.getOperatorId());
        if (opStations != null) {
            opStations.remove(station);
        }

        saveStations();
        player.sendMessage("§aUsunięto stację §e" + stationId);
        return true;
    }

    // Znajdź najlepszą stację dla gracza
    public Station findBestStation(Player player, String operatorId) {
        List<Station> opStations = operatorStations.get(operatorId);
        if (opStations == null || opStations.isEmpty()) return null;

        Station bestStation = null;
        double bestQuality = -1;

        for (Station station : opStations) {
            if (!station.isActive() || station.isBroken()) continue;

            double quality = station.getSignalQuality(player.getLocation());
            if (quality > bestQuality) {
                bestQuality = quality;
                bestStation = station;
            }
        }

        return bestStation;
    }

    // Znajdź stację z najlepszym internetem
    public Station findBestInternetStation(Player player, String operatorId) {
        List<Station> opStations = operatorStations.get(operatorId);
        if (opStations == null || opStations.isEmpty()) return null;

        Station bestStation = null;
        double bestSpeed = -1;

        for (Station station : opStations) {
            if (!station.isActive() || station.isBroken()) continue;
            if (!station.supportsInternet()) continue;

            double distance = station.getLocation().distance(player.getLocation());
            double speed = station.getSpeedAtDistance(distance);

            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestStation = station;
            }
        }

        return bestStation;
    }

    // Sprawdź czy gracz jest w zasięgu operatora
    public boolean isPlayerInRange(Player player, String operatorId) {
        return findBestStation(player, operatorId) != null;
    }

    // Sprawdź czy technologia obsługuje internet (kompatybilność)
    public boolean supportsInternet(String technology) {
        return isInternetSupported(technology);
    }

    // Pobierz prędkość internetu dla gracza
    public double getPlayerInternetSpeed(Player player, String operatorId) {
        Station station = findBestInternetStation(player, operatorId);
        if (station == null) return 0.0;

        double distance = station.getLocation().distance(player.getLocation());
        return station.getSpeedAtDistance(distance);
    }

    // System awarii
    private void startFailureCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkForFailures();
            }
        }.runTaskTimer(plugin, 20 * 60 * 30, 20 * 60 * 30); // Co 30 minut
    }

    private void checkForFailures() {
        Random random = new Random();
        int brokenCount = 0;

        for (Station station : stations.values()) {
            if (!station.isActive() || station.isBroken()) continue;

            double chance = station.getDamageChance();
            if (random.nextDouble() * 100 < chance) {
                station.setBroken(true);
                station.setActive(false);
                brokenCount++;

                Player owner = Bukkit.getPlayer(station.getCreatedBy());
                if (owner != null && owner.isOnline()) {
                    owner.sendMessage("§c⚠ Twoja stacja §e" + station.getId() + " §c(Technologia: " +
                            station.getTechnology() + ") uległa awarii! Koszt naprawy: $" + station.getRepairCost());
                }
            }
        }

        if (brokenCount > 0) {
            saveStations();
            plugin.getLogger().info("Awaria " + brokenCount + " stacji bazowych!");
        }
    }

    // Gettery
    public Station getStation(String id) {
        return stations.get(id);
    }

    public List<Station> getOperatorStations(String operatorId) {
        return operatorStations.getOrDefault(operatorId, new ArrayList<>());
    }

    public Map<String, Station> getAllStations() {
        return new HashMap<>(stations);
    }

    public FileConfiguration getTechnologiesConfig() {
        return technologiesConfig;
    }

    // Zapis i odczyt
    public void saveStations() {
        FileConfiguration config = new YamlConfiguration();

        for (Station station : stations.values()) {
            String path = "stations." + station.getId();
            config.set(path + ".operatorId", station.getOperatorId());
            config.set(path + ".location", station.locationToString());
            config.set(path + ".level", station.getLevel());
            config.set(path + ".technology", station.getTechnology());
            config.set(path + ".active", station.isActive());
            config.set(path + ".broken", station.isBroken());
            config.set(path + ".createdBy", station.getCreatedBy().toString());
            config.set(path + ".creationDate", station.getCreationDate());
        }

        try {
            config.save(stationsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Nie można zapisać stations.yml! " + e.getMessage());
        }
    }

    public void saveAll() {
        saveStations();
    }

    private void loadStations() {
        if (!stationsFile.exists()) {
            try {
                stationsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Nie można utworzyć stations.yml! " + e.getMessage());
            }
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(stationsFile);
        ConfigurationSection stationsSection = config.getConfigurationSection("stations");

        if (stationsSection == null) return;

        for (String id : stationsSection.getKeys(false)) {
            String path = "stations." + id;

            String operatorId = config.getString(path + ".operatorId");
            String locationStr = config.getString(path + ".location");
            Location location = Station.stringToLocation(locationStr);

            if (location == null) continue;

            String technology = config.getString(path + ".technology", "LTE");
            UUID createdBy = UUID.fromString(config.getString(path + ".createdBy"));

            Station station = new Station(id, operatorId, location, technology, createdBy, this);
            station.setLevel(config.getInt(path + ".level", 1));
            station.setActive(config.getBoolean(path + ".active", true));
            station.setBroken(config.getBoolean(path + ".broken", false));

            stations.put(id, station);
            operatorStations.computeIfAbsent(operatorId, k -> new ArrayList<>()).add(station);
        }

        plugin.getLogger().info("Załadowano " + stations.size() + " stacji bazowych.");
    }

    public void reloadTechnologies() {
        this.technologiesConfig = YamlConfiguration.loadConfiguration(technologiesFile);
    }
}
