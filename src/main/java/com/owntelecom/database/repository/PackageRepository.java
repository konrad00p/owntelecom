package com.owntelecom.database.repository;

import com.owntelecom.database.DatabaseManager;

import java.sql.*;
import java.util.*;

public class PackageRepository {

    public record ServicePackage(int id, String operatorId, String name, double price,
                                 double minutes, double sms, double mb, int durationDays, String zoneId) {
    }

    private final DatabaseManager db;

    public PackageRepository(DatabaseManager db) {
        this.db = db;
    }

    public List<ServicePackage> findByOperator(String operatorId) {
        return db.runSync(conn -> {
            List<ServicePackage> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM packages WHERE operator_id = ?")) {
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

    public Optional<ServicePackage> findById(int id) {
        return db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM packages WHERE id = ?")) {
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

    public int create(ServicePackage pkg) {
        return db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO packages (operator_id, name, price, minutes, sms, mb, duration_days, zone_id) VALUES (?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, pkg.operatorId().toLowerCase(Locale.ROOT));
                ps.setString(2, pkg.name());
                ps.setDouble(3, pkg.price());
                ps.setDouble(4, pkg.minutes());
                ps.setDouble(5, pkg.sms());
                ps.setDouble(6, pkg.mb());
                ps.setInt(7, pkg.durationDays());
                ps.setString(8, pkg.zoneId());
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

    private ServicePackage map(ResultSet rs) throws SQLException {
        return new ServicePackage(
                rs.getInt("id"),
                rs.getString("operator_id"),
                rs.getString("name"),
                rs.getDouble("price"),
                rs.getDouble("minutes"),
                rs.getDouble("sms"),
                rs.getDouble("mb"),
                rs.setInt("duration_days"),
                rs.getString("zone_id")
        );
    }
}
