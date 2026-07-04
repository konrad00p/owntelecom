package com.owntelecom.database.repository;

import com.owntelecom.database.DatabaseManager;
import com.owntelecom.database.model.Website;

import java.sql.*;
import java.util.*;

public class WebsiteRepository {

    private final DatabaseManager db;

    public WebsiteRepository(DatabaseManager db) {
        this.db = db;
    }

    public List<Website> findEnabled() {
        return db.runSync(conn -> {
            List<Website> list = new ArrayList<>();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM websites WHERE enabled = 1 AND broken = 0")) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        });
    }

    public Optional<Website> findBySlug(String slug) {
        return db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM websites WHERE slug = ?")) {
                ps.setString(1, slug.toLowerCase(Locale.ROOT));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(map(rs));
                    }
                }
            }
            return Optional.empty();
        });
    }

    public int create(Website website) {
        return db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO websites (slug, owner_uuid, operator_id, server_id, title, enabled, broken, template) VALUES (?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, website.slug().toLowerCase(Locale.ROOT));
                ps.setString(2, website.ownerUuid().toString());
                ps.setString(3, website.operatorId());
                if (website.serverId() != null) {
                    ps.setInt(4, website.serverId());
                } else {
                    ps.setNull(4, Types.INTEGER);
                }
                ps.setString(5, website.title());
                ps.setInt(6, website.enabled() ? 1 : 0);
                ps.setInt(7, website.broken() ? 1 : 0);
                ps.setString(8, website.template());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
            return -1;
        });
    }

    public List<String> getLines(int websiteId) {
        return db.runSync(conn -> {
            List<String> lines = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT content FROM website_lines WHERE website_id = ? ORDER BY sort_order")) {
                ps.setInt(1, websiteId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lines.add(rs.getString("content"));
                    }
                }
            }
            return lines;
        });
    }

    public void setLines(int websiteId, List<String> lines) {
        db.runSync(conn -> {
            try (PreparedStatement del = conn.prepareStatement("DELETE FROM website_lines WHERE website_id = ?")) {
                del.setInt(1, websiteId);
                del.executeUpdate();
            }
            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO website_lines (website_id, sort_order, content) VALUES (?,?,?)")) {
                for (int i = 0; i < lines.size(); i++) {
                    ins.setInt(1, websiteId);
                    ins.setInt(2, i);
                    ins.setString(3, lines.get(i));
                    ins.addBatch();
                }
                ins.executeBatch();
            }
            return null;
        });
    }

    public void setEnabled(int id, boolean enabled) {
        db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE websites SET enabled = ? WHERE id = ?")) {
                ps.setInt(1, enabled ? 1 : 0);
                ps.setInt(2, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    private Website map(ResultSet rs) throws SQLException {
        int serverId = rs.getInt("server_id");
        return new Website(
                rs.getInt("id"),
                rs.getString("slug"),
                UUID.fromString(rs.getString("owner_uuid")),
                rs.getString("operator_id"),
                rs.wasNull() ? null : serverId,
                rs.getString("title"),
                rs.getInt("enabled") == 1,
                rs.getInt("broken") == 1,
                rs.getString("template")
        );
    }
}
