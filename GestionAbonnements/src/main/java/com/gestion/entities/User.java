package com.gestion.entities;

/**
 * Entity class representing a User in the database.
 * Contains: id, name, email, password, role, phone, motorized, image.
 */
public class User {
    private int id;
    private String name;
    private String email;
    private String password;
    private String role; // "USER" or "ADMIN"
    private String phone; // ✅ NEW: Phone number
    private String motorized; // "YES" or "NO"
    private String image; // Profile image path

    // Default Constructor
    public User() {
    }

    // Constructor without ID (for new users)
    public User(String name, String email, String password, String role, String phone, String motorized, String image) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.phone = phone;
        this.motorized = motorized;
        this.image = image;
    }

    // Constructor with ID (for existing users)
    public User(int id, String name, String email, String password, String role, String phone, String motorized, String image) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.phone = phone;
        this.motorized = motorized;
        this.image = image;
    }

    // --- Getters and Setters ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMotorized() {
        return motorized;
    }

    public void setMotorized(String motorized) {
        this.motorized = motorized;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    // --- toString ---

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", role='" + role + '\'' +
                ", motorized='" + motorized + '\'' +
                '}';
    }
}