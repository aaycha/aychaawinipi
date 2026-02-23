package com.gestion.services;

import com.gestion.entities.Notification;
import com.gestion.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {

    public void create(Notification notification) throws SQLException {
        String sql = "INSERT INTO notifications (user_id, title, message, date, is_read, type) VALUES (?, ?, ?, ?, ?, ?)";
        Connection connection = MyConnection.getConnectionStatic();
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setLong(1, notification.getUserId());
        ps.setString(2, notification.getTitle());
        ps.setString(3, notification.getMessage());
        ps.setTimestamp(4, Timestamp.valueOf(notification.getDate()));
        ps.setBoolean(5, notification.isRead());
        ps.setString(6, notification.getType().name());
        ps.executeUpdate();
    }

    public List<Notification> getByUserId(Long userId) throws SQLException {
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY date DESC";
        Connection connection = MyConnection.getConnectionStatic();
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setLong(1, userId);
        ResultSet rs = ps.executeQuery();
        List<Notification> notifications = new ArrayList<>();
        while (rs.next()) {
            Notification n = new Notification();
            n.setId(rs.getLong("id"));
            n.setUserId(rs.getLong("user_id"));
            n.setTitle(rs.getString("title"));
            n.setMessage(rs.getString("message"));
            n.setDate(rs.getTimestamp("date").toLocalDateTime());
            n.setRead(rs.getBoolean("is_read"));
            n.setType(Notification.NotificationType.valueOf(rs.getString("type")));
            notifications.add(n);
        }
        return notifications;
    }

    public void markAsRead(Long id) throws SQLException {
        String sql = "UPDATE notifications SET is_read = true WHERE id = ?";
        Connection connection = MyConnection.getConnectionStatic();
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setLong(1, id);
        ps.executeUpdate();
    }
}
