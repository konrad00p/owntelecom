package com.owntelecom.database.repository;

import com.owntelecom.database.DatabaseManager;
import com.owntelecom.database.model.Subscriber;

import java.sql.*;
import java.util.*;

public class SubscriberRepository {

    private final DatabaseManager db;

    public SubscriberRepository(DatabaseManager db) {
        this.db = db;
    }

    public Optional<Subscriber> findByPlayer(UUID playerUuid) {
        return db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM subscribers WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(map(rs));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error executing findByPlayer", e);
            }
            return Optional.empty();
        });
    }

    public List<Subscriber> findByOperator(String operatorId) {
        return db.runSync(conn -> {
            List<Subscriber> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM subscribers WHERE operator_id = ?")) {
                ps.setString(1, operatorId.toLowerCase(Locale.ROOT));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(map(rs));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error executing findByOperator", e);
            }
            return list;
        });
    }

    public void setOperator(UUID playerUuid, String operatorId) {
        db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO subscribers (player_uuid, operator_id) VALUES (?,?) ON CONFLICT(player_uuid) DO UPDATE SET operator_id = excluded.operator_id")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, operatorId.toLowerCase(Locale.ROOT));
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Error executing setOperator", e);
            }
            return null;
        });
    }

    public void clearOperator(UUID playerUuid) {
        db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM subscribers WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Error executing clearOperator", e);
            }
            return null;
        });
    }

    public void updatePackage(Subscriber subscriber) {
        db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                    UPDATE subscribers SET package_id = ?, package_minutes_left = ?, package_sms_left = ?,
                    package_mb_left = ?, package_expires_at = ? WHERE player_uuid = ?
                    """)) {
                if (subscriber.packageId() != null) {
                    ps.setInt(1, subscriber.packageId());
                } else {
                    ps.setNull(1, Types.INTEGER);
                }
                ps.setDouble(2, subscriber.packageMinutesLeft());
                ps.setDouble(3, subscriber.packageSmsLeft());
                ps.setDouble(4, subscriber.packageMbLeft());
                ps.setLong(5, subscriber.packageExpiresAt());
                ps.setString(6, subscriber.playerUuid().toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Error executing updatePackage", e);
            }
            return null;
        });
    }

    private Subscriber map(ResultSet rs) throws SQLException {
        int pkgId = rs.getInt("package_id");
        return new Subscriber(
                UUID.fromString(rs.getString("player_uuid")),
                rs.getString("operator_id"),
                rs.wasNull() ? null : pkgId,
                rs.getDouble("package_minutes_left"),
                rs.getDouble("package_sms_left"),
                rs.getDouble("package_mb_left"),
                rs.getLong("package_expires_at")
        );
    }
}
