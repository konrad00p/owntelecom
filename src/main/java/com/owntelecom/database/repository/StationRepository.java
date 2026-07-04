package com.owntelecom.database.repository;

import com.owntelecom.database.DatabaseManager;
import com.owntelecom.database.model.Station;

import java.sql.*;
import java.util.*;

public class StationRepository {

    private final DatabaseManager db;

    public StationRepository(DatabaseManager db) {
        this.db = db;
    }

    public List<Station> findByOperator(String operatorId) {
        return db.runSync(conn -> {
            List<Station> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM stations WHERE operator_id = ?")) {
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

    public List<Station> findByWorld(String world) {
        return db.runSync(conn -> {
            List<Station> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM stations WHERE world = ? AND broken = 0")) {
                ps.setString(1, world);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(map(rs));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error executing findByWorld", e);
            }
            return list;
        });
    }

    public Optional<Station> findById(int id) {
        return db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM stations WHERE id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(map(rs));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error executing findById", e);
            }
            return Optional.empty();
        });
    }

    public int create(Station station) {
        return db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO stations (operator_id, world, x, y, z, technology, level, broken, last_maintenance) VALUES (?,?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, station.operatorId().toLowerCase(Locale.ROOT));
                ps.setString(2, station.world());
                ps.setInt(3, station.x());
                ps.setInt(4, station.y());
                ps.setInt(5, station.z());
                ps.setString(6, station.technology());
                ps.setInt(7, station.level());
                ps.setInt(8, station.broken() ? 1 : 0);
                ps.setLong(9, station.lastMaintenance());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error executing create", e);
            }
            return -1;
        });
    }

    public void setBroken(int id, boolean broken) {
        db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE stations SET broken = ? WHERE id = ?")) {
                ps.setInt(1, broken ? 1 : 0);
                ps.setInt(2, id);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Error executing setBroken", e);
            }
            return null;
        });
    }

    public void setLevel(int id, int level) {
        db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE stations SET level = ? WHERE id = ?")) {
                ps.setInt(1, level);
                ps.setInt(2, id);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Error executing setLevel", e);
            }
            return null;
        });
    }

    public void delete(int id) {
        db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM stations WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Error executing delete", e);
            }
            return null;
        });
    }

    public void deleteByOperator(String operatorId) {
        db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM stations WHERE operator_id = ?")) {
                ps.setString(1, operatorId.toLowerCase(Locale.ROOT));
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Error executing deleteByOperator", e);
            }
            return null;
        });
    }

    private Station map(ResultSet rs) throws SQLException {
        return new Station(
                rs.getInt("id"),
                rs.getString("operator_id"),
                rs.getString("world"),
                rs.getInt("x"),
                rs.getInt("y"),
                rs.getInt("z"),
                rs.getString("technology"),
                rs.getInt("level"),
                rs.getInt("broken") == 1,
                rs.getLong("last_maintenance")
        );
    }
}
