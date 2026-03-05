package com.gestion.services;

import com.gestion.entities.Comment;
import com.gestion.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentDAO {

    private final Connection cnx;

    public CommentDAO() {
        cnx = MyConnection.getInstance().getConnection();
        ensureTableExists();
    }

    private void ensureTableExists() {
        String sql = "CREATE TABLE IF NOT EXISTS comments (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "post_id INT NOT NULL, " +
                "content TEXT NOT NULL, " +
                "FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE" +
                ")";

        try (Statement st = cnx.createStatement()) {
            st.executeUpdate(sql);

            // Sécurise AUTO_INCREMENT
            try {
                st.executeUpdate("ALTER TABLE comments MODIFY id INT AUTO_INCREMENT");
            } catch (SQLException ignored) {
                // Déjà configuré
            }

        } catch (SQLException ex) {
            System.err.println("❌ CommentDAO.ensureTableExists: " + ex.getMessage());
        }
    }

    public void add(Comment comment) {
        String sql = "INSERT INTO comments (post_id, content) VALUES (?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, comment.getPostId());
            ps.setString(2, comment.getContent());
            ps.executeUpdate();

        } catch (SQLException ex) {
            System.err.println("❌ CommentDAO.add: " + ex.getMessage());
        }
    }

    public List<Comment> getByPostId(int postId) {
        List<Comment> list = new ArrayList<>();
        String sql = "SELECT id, post_id, content FROM comments WHERE post_id = ? ORDER BY id";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, postId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Comment c = new Comment();
                    c.setId(rs.getInt("id"));
                    c.setPostId(rs.getInt("post_id"));
                    c.setContent(rs.getString("content"));
                    list.add(c);
                }
            }

        } catch (SQLException ex) {
            System.err.println("❌ CommentDAO.getByPostId: " + ex.getMessage());
        }

        return list;
    }

    public void update(int id, String content) {
        String sql = "UPDATE comments SET content = ? WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, content);
            ps.setInt(2, id);
            ps.executeUpdate();

        } catch (SQLException ex) {
            System.err.println("❌ CommentDAO.update: " + ex.getMessage());
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM comments WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException ex) {
            System.err.println("❌ CommentDAO.delete: " + ex.getMessage());
        }
    }

    public void deleteByPostId(int postId) {
        String sql = "DELETE FROM comments WHERE post_id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.executeUpdate();

        } catch (SQLException ex) {
            System.err.println("❌ CommentDAO.deleteByPostId: " + ex.getMessage());
        }
    }
}