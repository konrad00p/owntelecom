package com.owntelecom.database;

import com.owntelecom.OwnTelecomPlugin;
import com.owntelecom.config.ConfigManager;
import com.owntelecom.database.repository.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class DatabaseManager {

    private final OwnTelecomPlugin plugin;
    private final ConfigManager configManager;
    private HikariDataSource dataSource;
    private ExecutorService executor;

    private OperatorRepository operatorRepository;
    private StationRepository stationRepository;
    private SubscriberRepository subscriberRepository;
    private AgreementRepository agreementRepository;
    private PackageRepository packageRepository;
    private WebsiteRepository websiteRepository;
    private PlayerMetaRepository playerMetaRepository;

    public DatabaseManager(OwnTelecomPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void init() {
        int poolSize = configManager.getConfig().getInt("database.pool-size", 4);
        executor = Executors.newFixedThreadPool(poolSize);

        HikariConfig hikari = new HikariConfig();
        File dbFile = new File(plugin.getDataFolder(), configManager.getConfig().getString("database.file", "owntelecom.db"));
        hikari.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        hikari.setMaximumPoolSize(poolSize);
        hikari.setPoolName("OwnTelecom-Pool");
        hikari.addDataSourceProperty("cachePrepStmts", "true");
        hikari.addDataSourceProperty("prepStmtCacheSize", "250");

        dataSource = new HikariDataSource(hikari);
        createSchema();

        operatorRepository = new OperatorRepository(this);
        stationRepository = new StationRepository(this);
        subscriberRepository = new SubscriberRepository(this);
        agreementRepository = new AgreementRepository(this);
        packageRepository = new PackageRepository(this);
        websiteRepository = new WebsiteRepository(this);
        playerMetaRepository = new PlayerMetaRepository(this);
    }

    private void createSchema() {
        runSync(conn -> {
            try (Statement st = conn.createStatement()) {
                st.execute("""
                    CREATE TABLE IF NOT EXISTS operators (
                        id TEXT PRIMARY KEY,
                        display_name TEXT NOT NULL,
                        owner_uuid TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        balance REAL DEFAULT 0,
                        prepaid_minute REAL DEFAULT 1.0,
                        prepaid_sms REAL DEFAULT 0.5,
                        prepaid_mb REAL DEFAULT 0.1,
                        pass_cost_to_client INTEGER DEFAULT 1
                    )
                    """);
                st.execute("""
                    CREATE TABLE IF NOT EXISTS stations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        operator_id TEXT NOT NULL,
                        world TEXT NOT NULL,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        z INTEGER NOT NULL,
                        technology TEXT NOT NULL,
                        level INTEGER DEFAULT 1,
                        broken INTEGER DEFAULT 0,
                        last_maintenance INTEGER DEFAULT 0,
                        FOREIGN KEY (operator_id) REFERENCES operators(id)
                    )
                    """);
                st.execute("""
                    CREATE TABLE IF NOT EXISTS subscribers (
                        player_uuid TEXT PRIMARY KEY,
                        operator_id TEXT NOT NULL,
                        package_id INTEGER,
                        package_minutes_left REAL DEFAULT 0,
                        package_sms_left REAL DEFAULT 0,
                        package_mb_left REAL DEFAULT 0,
                        package_expires_at INTEGER DEFAULT 0,
                        FOREIGN KEY (operator_id) REFERENCES operators(id)
                    )
                    """);
                st.execute("""
                    CREATE TABLE IF NOT EXISTS agreements (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        operator_a TEXT NOT NULL,
                        operator_b TEXT NOT NULL,
                        type TEXT NOT NULL,
                        roaming_minute REAL DEFAULT 0,
                        roaming_sms REAL DEFAULT 0,
                        roaming_mb REAL DEFAULT 0,
                        call_minute REAL DEFAULT 0,
                        call_sms REAL DEFAULT 0,
                        pass_roaming_to_client INTEGER DEFAULT 0,
                        pass_call_to_client INTEGER DEFAULT 0,
                        active INTEGER DEFAULT 1,
                        UNIQUE(operator_a, operator_b, type)
                    )
                    """);
                st.execute("""
                    CREATE TABLE IF NOT EXISTS zones (
                        id TEXT PRIMARY KEY,
                        operator_id TEXT NOT NULL,
                        type TEXT NOT NULL,
                        display_name TEXT NOT NULL,
                        extra_minute REAL DEFAULT 0,
                        extra_sms REAL DEFAULT 0,
                        extra_mb REAL DEFAULT 0,
                        FOREIGN KEY (operator_id) REFERENCES operators(id)
                    )
                    """);
                st.execute("""
                    CREATE TABLE IF NOT EXISTS zone_members (
                        zone_id TEXT NOT NULL,
                        target_operator_id TEXT NOT NULL,
                        PRIMARY KEY (zone_id, target_operator_id)
                    )
                    """);
                st.execute("""
                    CREATE TABLE IF NOT EXISTS packages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        operator_id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        price REAL NOT NULL,
                        minutes REAL DEFAULT 0,
                        sms REAL DEFAULT 0,
                        mb REAL DEFAULT 0,
                        duration_days INTEGER DEFAULT 30,
                        zone_id TEXT,
                        FOREIGN KEY (operator_id) REFERENCES operators(id)
                    )
                    """);
                st.execute("""
                    CREATE TABLE IF NOT EXISTS websites (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        slug TEXT UNIQUE NOT NULL,
                        owner_uuid TEXT NOT NULL,
                        operator_id TEXT,
                        server_id INTEGER,
                        title TEXT NOT NULL,
                        enabled INTEGER DEFAULT 1,
                        broken INTEGER DEFAULT 0,
                        template TEXT DEFAULT 'default'
                    )
                    """);
                st.execute("""
                    CREATE TABLE IF NOT EXISTS website_lines (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        website_id INTEGER NOT NULL,
                        sort_order INTEGER NOT NULL,
                        content TEXT NOT NULL,
                        FOREIGN KEY (website_id) REFERENCES websites(id)
                    )
                    """);
                st.execute("""
                    CREATE TABLE IF NOT EXISTS datacenters (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        owner_uuid TEXT NOT NULL,
                        world TEXT NOT NULL,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        z INTEGER NOT NULL,
                        level INTEGER DEFAULT 1,
                        broken INTEGER DEFAULT 0
                    )
                    """);
                st.execute("""
                    CREATE TABLE IF NOT EXISTS player_meta (
                        player_uuid TEXT PRIMARY KEY,
                        last_operator_create INTEGER DEFAULT 0,
                        has_created_operator INTEGER DEFAULT 0
                    )
                    """);
                st.execute("CREATE INDEX IF NOT EXISTS idx_stations_operator ON stations(operator_id)");
                st.execute("CREATE INDEX IF NOT EXISTS idx_stations_world ON stations(world, x, z)");
                st.execute("CREATE INDEX IF NOT EXISTS idx_subscribers_operator ON subscribers(operator_id)");
            } catch (SQLException e) {
                throw new RuntimeException("Error creating database schema", e);
            }
            return null;
        });
    }

    public <T> CompletableFuture<T> runAsync(Function<Connection, T> task) {
        return CompletableFuture.supplyAsync(() -> runSync(task), executor);
    }

    public <T> T runSync(Function<Connection, T> task) {
        try (Connection conn = dataSource.getConnection()) {
            return task.apply(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        }
    }

    public void runAsyncVoid(java.util.function.Consumer<Connection> task) {
        runAsync(conn -> {
            task.accept(conn);
            return null;
        });
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public OperatorRepository operators() {
        return operatorRepository;
    }

    public StationRepository stations() {
        return stationRepository;
    }

    public SubscriberRepository subscribers() {
        return subscriberRepository;
    }

    public AgreementRepository agreements() {
        return agreementRepository;
    }

    public PackageRepository packages() {
        return packageRepository;
    }

    public WebsiteRepository websites() {
        return websiteRepository;
    }

    public PlayerMetaRepository playerMeta() {
        return playerMetaRepository;
    }
}
