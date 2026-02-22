package com.gestion.services;

import com.gestion.entities.User;
import com.gestion.interfaces.IService;
import com.gestion.tools.MyConnection;
import com.gestion.tools.PasswordHasher;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IService<User> {

    public UserService() {
        // Connection handled dynamically
    }

    // ------------------ ADD USER ------------------
    @Override
    public void ajouter(User user) throws SQLException {

        String sql = "INSERT INTO users (name, email, password, role, phone, motorized, image) VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection connection = MyConnection.getConnectionStatic();
        if (connection == null)
            throw new SQLException("Connexion à la base de données indisponible.");
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, user.getName());
        preparedStatement.setString(2, user.getEmail());
        preparedStatement.setString(3, user.getPassword()); // ✅ Already hashed
        preparedStatement.setString(4, user.getRole());
        preparedStatement.setString(5, user.getPhone()); // ✅ NEW
        preparedStatement.setString(6, user.getMotorized());
        preparedStatement.setString(7, user.getImage());

        preparedStatement.executeUpdate();
        System.out.println("✅ User added successfully!");
    }

    // ------------------ UPDATE USER ------------------
    @Override
    public void modifier(User user) throws SQLException {

        String sql = "UPDATE users SET name=?, email=?, password=?, role=?, phone=?, motorized=?, image=? WHERE id=?";
        Connection connection = MyConnection.getConnectionStatic();
        if (connection == null)
            throw new SQLException("Connexion à la base de données indisponible.");
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, user.getName());
        preparedStatement.setString(2, user.getEmail());
        preparedStatement.setString(3, user.getPassword()); // ✅ Already hashed
        preparedStatement.setString(4, user.getRole());
        preparedStatement.setString(5, user.getPhone()); // ✅ NEW
        preparedStatement.setString(6, user.getMotorized());
        preparedStatement.setString(7, user.getImage());
        preparedStatement.setInt(8, user.getId());

        preparedStatement.executeUpdate();
        System.out.println("✅ User updated successfully!");
    }

    // ------------------ DELETE USER ------------------
    @Override
    public void supprimer(int id) throws SQLException {

        String sql = "DELETE FROM users WHERE id = ?";
        Connection connection = MyConnection.getConnectionStatic();
        if (connection == null)
            throw new SQLException("Connexion à la base de données indisponible.");
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setInt(1, id);

        preparedStatement.executeUpdate();
        System.out.println("✅ User deleted successfully!");
    }

    // ------------------ GET ALL USERS ------------------
    @Override
    public List<User> recuperer() throws SQLException {

        String sql = "SELECT * FROM users";
        Connection connection = MyConnection.getConnectionStatic();
        if (connection == null)
            throw new SQLException("Connexion à la base de données indisponible.");
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);

        List<User> users = new ArrayList<>();

        while (rs.next()) {

            User user = new User();
            user.setId(rs.getInt("id"));
            user.setName(rs.getString("name"));
            user.setEmail(rs.getString("email"));
            user.setPassword(rs.getString("password"));
            user.setRole(rs.getString("role"));
            user.setPhone(rs.getString("phone")); // ✅ NEW
            user.setMotorized(rs.getString("motorized"));
            user.setImage(rs.getString("image"));

            users.add(user);
        }

        return users;
    }

    // ------------------ LOGIN / VERIFY PASSWORD ------------------
    public User login(String email, String plainPassword) throws SQLException {

        String sql = "SELECT * FROM users WHERE email = ?";
        Connection connection = MyConnection.getConnectionStatic();
        if (connection == null)
            throw new SQLException("Connexion à la base de données indisponible.");
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, email);

        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) {
            String hashedPasswordFromDB = rs.getString("password");

            // ✅ VERIFY PASSWORD USING BCRYPT
            if (PasswordHasher.verifyPassword(plainPassword, hashedPasswordFromDB)) {

                // Password is correct, return user
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setPassword(hashedPasswordFromDB);
                user.setRole(rs.getString("role"));
                user.setPhone(rs.getString("phone")); // ✅ NEW
                user.setMotorized(rs.getString("motorized"));
                user.setImage(rs.getString("image"));

                System.out.println("✅ Login successful for: " + user.getName());
                return user;
            } else {
                System.out.println("❌ Password verification failed");
            }
        } else {
            System.out.println("❌ User not found with email: " + email);
        }

        return null;
    }

    // ------------------ CHECK IF EMAIL EXISTS ------------------
    public boolean emailExists(String email) throws SQLException {

        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        Connection connection = MyConnection.getConnectionStatic();
        if (connection == null)
            throw new SQLException("Connexion à la base de données indisponible.");
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, email);

        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) {
            return rs.getInt(1) > 0;
        }

        return false;
    }
}