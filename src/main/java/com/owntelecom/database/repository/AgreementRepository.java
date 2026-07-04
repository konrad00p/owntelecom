package com.owntelecom.database.repository;

import com.owntelecom.database.DatabaseManager;
import com.owntelecom.database.model.Agreement;
import com.owntelecom.database.model.AgreementType;

import java.sql.*;
import java.util.*;

public class AgreementRepository {

    private final DatabaseManager db;

    public AgreementRepository(DatabaseManager db) {
        this.db = db;
    }

    public Optional<Agreement> find(String operatorA, String operatorB, AgreementType type) {
        String a = operatorA.toLowerCase(Locale.ROOT);
        String b = operatorB.toLowerCase(Locale.ROOT);
        return db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM agreements WHERE active = 1 AND type = ? AND ((operator_a = ? AND operator_b = ?) OR (operator_a = ? AND operator_b = ?))")) {
                ps.setString(1, type.name());
                ps.setString(2, a);
                ps.setString(3, b);
                ps.setString(4, b);
                ps.setString(5, a);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(map(rs));
                    }
                }
            }
            return Optional.empty();
        });
    }

    public void create(Agreement agreement) {
        db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT OR REPLACE INTO agreements (operator_a, operator_b, type, roaming_minute, roaming_sms, roaming_mb,
                    call_minute, call_sms, pass_roaming_to_client, pass_call_to_client, active)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?)
                    """)) {
                ps.setString(1, agreement.operatorA().toLowerCase(Locale.ROOT));
                ps.setString(2, agreement.operatorB().toLowerCase(Locale.ROOT));
                ps.setString(3, agreement.type().name());
                ps.setDouble(4, agreement.roamingMinute());
                ps.setDouble(5, agreement.roamingSms());
                ps.setDouble(6, agreement.roamingMb());
                ps.setDouble(7, agreement.callMinute());
                ps.setDouble(8, agreement.callSms());
                ps.setInt(9, agreement.passRoamingToClient() ? 1 : 0);
                ps.setInt(10, agreement.passCallToClient() ? 1 : 0);
                ps.setInt(11, agreement.active() ? 1 : 0);
                ps.executeUpdate();
            }
            return null;
        });
    }

    private Agreement map(ResultSet rs) throws SQLException {
        return new Agreement(
                rs.getInt("id"),
                rs.getString("operator_a"),
                rs.getString("operator_b"),
                AgreementType.valueOf(rs.getString("type")),
                rs.getDouble("roaming_minute"),
                rs.getDouble("roaming_sms"),
                rs.getDouble("roaming_mb"),
                rs.getDouble("call_minute"),
                rs.getDouble("call_sms"),
                rs.getInt("pass_roaming_to_client") == 1,
                rs.getInt("pass_call_to_client") == 1,
                rs.getInt("active") == 1
        );
    }
}
