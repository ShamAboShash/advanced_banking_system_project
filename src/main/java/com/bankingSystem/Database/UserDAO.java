// src/main/java/com/bankingSystem/Database/UserDAO.java
package com.bankingSystem.Database;

import com.bankingSystem.user.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private final Connection conn = DatabaseConnection.getInstance().getConnection();

    public void saveUser(User user) {
        String sql = "MERGE INTO Users KEY(id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUserId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setString(4, user.getPhoneNumber());  // after role
            pstmt.setString(5, user.getRole().getDisplayName());
            pstmt.executeUpdate();
            System.out.println("User saved: " + user);
        } catch (SQLException e) {
            System.err.println("Error saving user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public User loadUser(String username) {
        String sql = "SELECT * FROM Users WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return buildUserFromResultSet(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public User loadUserById(String userId) {
        String sql = "SELECT * FROM Users WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return buildUserFromResultSet(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private User buildUserFromResultSet(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String username = rs.getString("username");
        String passwordHash = rs.getString("password");
        String phone = rs.getString("phoneNumber");
        String roleStr = rs.getString("role");

        Role role = Role.fromString(roleStr);

        return switch (role) {
            case CUSTOMER -> new Customer(id, username, passwordHash,phone);
            case TELLER   -> new Teller(id, username, passwordHash,phone);
            case MANAGER  -> new Manager(id, username, passwordHash,phone);
            case ADMIN    -> new Admin(id, username, passwordHash,phone);
        };
    }

    // Add these methods to UserDAO.java

    public List<User> getAllUsersByRole(String roleName) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM Users WHERE role = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, roleName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                users.add(buildUserFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error loading users by role: " + roleName);
            e.printStackTrace();
        }
        return users;
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM Users";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(buildUserFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public void deleteUser(String userId) {
        String sql = "DELETE FROM Users WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.executeUpdate();
            System.out.println("User deleted: " + userId);
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
        }
    }

//
//    public User loadUserByPhoneAndPassword(String phone, String password) {
//        String sql = "SELECT * FROM Users WHERE phoneNumber = ?";
//        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
//            pstmt.setString(1, phone);
//            ResultSet rs = pstmt.executeQuery();
//            if (rs.next()) {
//                User user = buildUserFromResultSet(rs);
//                if (user.checkPassword(password)) {
//                    return user;
//                }
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

    public User loadUserByPhone(String phone) {
        String sql = "SELECT * FROM Users WHERE phoneNumber = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, phone);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return buildUserFromResultSet(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Add this method to UserDAO.java
    public User loadUserByPhoneAndPassword(String phoneNumber, String plainPassword) {
        String sql = "SELECT * FROM Users WHERE phoneNumber = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, phoneNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = buildUserFromResultSet(rs);
                    // Check password using your hash method
                    if (user.checkPassword(plainPassword)) {
                        return user;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error during login by phone: " + e.getMessage());
            e.printStackTrace();
        }
        return null; // Invalid phone or password
    }
}