package com.owntelecom.database.repository;

import com.owntelecom.database.DatabaseManager;
import com.owntelecom.database.model.PlayerMeta;

import java.sql.*;
import java.util.*;

public class PlayerMetaRepository {

    private final DatabaseManager db;

    public PlayerMetaRepository(DatabaseManager db) {
        this.db = db;
    }

    public PlayerMeta getOrCreate(UUID playerUuid) {
        return db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM player_meta WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return map(rs);
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO player_meta (player_uuid) VALUES (?)")) {
                ps.setString(1, playerUuid.toString());
                ps.executeUpdate();
            }
            return new PlayerMeta(playerUuid, 0, false);
        });
    }

    public void markOperatorCreated(UUID playerUuid) {
        db.runSync(conn -> {
            getOrCreate(playerUuid);
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE player_meta SET has_created_operator = 1, last_operator_create = ? WHERE player_uuid = ?")) {
                ps.setLong(1, System.currentTimeMillis());
                ps.setString(2, playerUuid.toString());
                ps.executeUpdate();
            }
            return null;
        });
    }

    private PlayerMeta map(ResultSet rs) throws SQLException {
        return new PlayerMeta(
                UUID.fromString(rs.getString("player_uuid")),
                rs.getLong("last_operator_create"),
                rs.getInt("has_created_operator") == 1
        );
    }
}
