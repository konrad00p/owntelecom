package com.owntelecom.database.repository;

import com.owntelecom.database.DatabaseManager;
import com.owntelecom.database.model.Operator;

import java.sql.*;
import java.util.*;

public class OperatorRepository {

    private final DatabaseManager db;

    public OperatorRepository(DatabaseManager db) {
        this.db = db;
    }

    public Optional<Operator> findById(String id) {
        return db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM operators WHERE id = ?")) {
                ps.setString(1, id.toLowerCase(Locale.ROOT));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(map(rs));
                    }
                }
            }
            return Optional.empty();
        });
    }

    public List<Operator> findByOwner(UUID ownerUuid) {
        return db.runSync(conn -> {
            List<Operator> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM operators WHERE owner_uuid = ?")) {
                ps.setString(1, ownerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(map(rs));
                    }
                }
            }
            return list;
        });
    }

    public List<Operator> findAll() {
        return db.runSync(conn -> {
            List<Operator> list = new ArrayList<>();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM operators")) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        });
    }

    public void create(Operator operator) {
        db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO operators (id, display_name, owner_uuid, created_at, balance, prepaid_minute, prepaid_sms, prepaid_mb, pass_cost_to_client) VALUES (?,?,?,?,?,?,?,?,?)")) {
                ps.setString(1, operator.id().toLowerCase(Locale.ROOT));
                ps.setString(2, operator.displayName());
                ps.setString(3, operator.ownerUuid().toString());
                ps.setLong(4, operator.createdAt());
                ps.setDouble(5, operator.balance());
                ps.setDouble(6, operator.prepaidMinute());
                ps.setDouble(7, operator.prepaidSms());
                ps.setDouble(8, operator.prepaidMb());
                ps.setInt(9, operator.passCostToClient() ? 1 : 0);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void delete(String id) {
        db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM operators WHERE id = ?")) {
                ps.setString(1, id.toLowerCase(Locale.ROOT));
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void updateOwner(String id, UUID newOwner) {
        db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE operators SET owner_uuid = ? WHERE id = ?")) {
                ps.setString(1, newOwner.toString());
                ps.setString(2, id.toLowerCase(Locale.ROOT));
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void updateDisplayName(String id, String displayName) {
        db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE operators SET display_name = ? WHERE id = ?")) {
                ps.setString(1, displayName);
                ps.setString(2, id.toLowerCase(Locale.ROOT));
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void updatePrepaidRates(String id, double minute, double sms, double mb) {
        db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE operators SET prepaid_minute = ?, prepaid_sms = ?, prepaid_mb = ? WHERE id = ?")) {
                ps.setDouble(1, minute);
                ps.setDouble(2, sms);
                ps.setDouble(3, mb);
                ps.setString(4, id.toLowerCase(Locale.ROOT));
                ps.executeUpdate();
            }
            return null;
        });
    }

    private Operator map(ResultSet rs) throws SQLException {
        return new Operator(
                rs.getString("id"),
                rs.getString("display_name"),
                UUID.fromString(rs.getString("owner_uuid")),
                rs.getLong("created_at"),
                rs.getDouble("balance"),
                rs.getDouble("prepaid_minute"),
                rs.getDouble("prepaid_sms"),
                rs.getDouble("prepaid_mb"),
                rs.getInt("pass_cost_to_client") == 1
        );
    }
}
