package com.bankingSystem.Database;

import com.bankingSystem.Notification.NotificationRecord;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NotificationDAO {
    private final Connection conn = DatabaseConnection.getInstance().getConnection();

    public void save(NotificationRecord n) {
        if (n.id == null) n.id = UUID.randomUUID().toString();
        if (n.createdAt == null) n.createdAt = LocalDateTime.now();

        String sql = """
            INSERT INTO Notifications
            (id, userId, role, type, message, transactionId, createdAt, delivered, readFlag)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, n.id);
            ps.setString(2, n.userId);
            ps.setString(3, n.role);
            ps.setString(4, n.type);
            ps.setString(5, n.message);
            ps.setString(6, n.transactionId);
            ps.setObject(7, n.createdAt);
            ps.setBoolean(8, n.delivered);
            ps.setBoolean(9, n.readFlag);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving notification", e);
        }
    }

    // real-time: fetch undelivered
    public List<NotificationRecord> loadUndelivered(String userId) {
        List<NotificationRecord> out = new ArrayList<>();
        String sql = """
            SELECT * FROM Notifications
            WHERE userId = ? AND delivered = FALSE
            ORDER BY createdAt ASC
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    public void markDelivered(List<String> ids) {
        if (ids.isEmpty()) return;
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            in.append("?");
            if (i < ids.size() - 1) in.append(",");
        }
        String sql = "UPDATE Notifications SET delivered = TRUE WHERE id IN (" + in + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) ps.setString(i + 1, ids.get(i));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // menu: view unread
    public List<NotificationRecord> loadUnread(String userId) {
        List<NotificationRecord> out = new ArrayList<>();
        String sql = """
            SELECT * FROM Notifications
            WHERE userId = ? AND readFlag = FALSE
            ORDER BY createdAt DESC
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    public void markRead(String id) {
        String sql = "UPDATE Notifications SET readFlag = TRUE WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private NotificationRecord map(ResultSet rs) throws SQLException {
        NotificationRecord n = new NotificationRecord();
        n.id = rs.getString("id");
        n.userId = rs.getString("userId");
        n.role = rs.getString("role");
        n.type = rs.getString("type");
        n.message = rs.getString("message");
        n.transactionId = rs.getString("transactionId");
        n.createdAt = rs.getObject("createdAt", LocalDateTime.class);
        n.delivered = rs.getBoolean("delivered");
        n.readFlag = rs.getBoolean("readFlag");
        return n;
    }

    public void printNotificationsForUser(String userId) {
        String sql = """
        SELECT * FROM Notifications
        WHERE userId = ?
        ORDER BY createdAt DESC
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {

                System.out.println("\n📦 ===== NOTIFICATIONS FROM DB =====");
                boolean found = false;

                while (rs.next()) {
                    found = true;
                    System.out.println("""
                    ----------------------------------
                    id        : %s
                    userId    : %s
                    role      : %s
                    type      : %s
                    message   : %s
                    txId      : %s
                    createdAt : %s
                    delivered : %s
                    readFlag  : %s
                    ----------------------------------
                    """.formatted(
                            rs.getString("id"),
                            rs.getString("userId"),
                            rs.getString("role"),
                            rs.getString("type"),
                            rs.getString("message"),
                            rs.getString("transactionId"),
                            rs.getObject("createdAt"),
                            rs.getBoolean("delivered"),
                            rs.getBoolean("readFlag")
                    ));
                }

                if (!found) {
                    System.out.println("⚠️ No notifications found for userId=" + userId);
                }

                System.out.println("📦 ===== END =====\n");

            }
        } catch (SQLException e) {
            throw new RuntimeException("Error printing notifications", e);
        }
    }

}
