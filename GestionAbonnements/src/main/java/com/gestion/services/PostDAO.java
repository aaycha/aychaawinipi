package com.gestion.services;

import com.gestion.entities.Post;
import com.gestion.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostDAO {

    private final Connection cnx;

    public PostDAO() {
        cnx = MyConnection.getInstance().getConnection();
        ensureTableExists();
    }

    private void ensureTableExists() {
        String sql = "CREATE TABLE IF NOT EXISTS posts (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "title VARCHAR(255) NOT NULL, " +
                "content TEXT NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        try (Statement st = cnx.createStatement()) {
            st.executeUpdate(sql);

            // Sécuriser AUTO_INCREMENT
            try {
                st.executeUpdate("ALTER TABLE posts MODIFY id INT AUTO_INCREMENT");
            } catch (SQLException ignored) {
            }

        } catch (SQLException ex) {
            System.err.println("❌ PostDAO.ensureTableExists: " + ex.getMessage());
        }
    }

    public void add(Post post) {
        String sql = "INSERT INTO posts (title, content) VALUES (?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, post.getTitle());
            ps.setString(2, post.getContent());
            ps.executeUpdate();

        } catch (SQLException ex) {
            System.err.println("❌ PostDAO.add: " + ex.getMessage());
        }
    }

    public List<Post> getAll() {
        List<Post> list = new ArrayList<>();
        String sql = "SELECT id, title, content, created_at FROM posts ORDER BY created_at DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Post post = new Post();
                post.setId(rs.getInt("id"));
                post.setTitle(rs.getString("title"));
                post.setContent(rs.getString("content"));

                if (rs.getTimestamp("created_at") != null) {
                    post.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                }

                list.add(post);
            }

        } catch (SQLException ex) {
            System.err.println("❌ PostDAO.getAll: " + ex.getMessage());
        }

        return list;
    }

    public void update(int id, String title, String content) {
        String sql = "UPDATE posts SET title = ?, content = ? WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, content);
            ps.setInt(3, id);
            ps.executeUpdate();

        } catch (SQLException ex) {
            System.err.println("❌ PostDAO.update: " + ex.getMessage());
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM posts WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException ex) {
            System.err.println("❌ PostDAO.delete: " + ex.getMessage());
        }
    }
}