// src/main/java/com/bankingSystem/user/User.java
package com.bankingSystem.user;

import com.bankingSystem.Database.UserDAO;
import org.mindrot.jbcrypt.BCrypt;  // ← NEW IMPORT

import java.util.UUID;

public abstract class User {
    protected String userId;
    protected String username;
    protected String passwordHash;  // Now stores BCrypt hash
    protected Role role;
    protected String phoneNumber;

    public User(String username, String password, String phoneNumber, Role role) {
        this.userId = UUID.randomUUID().toString();
        this.username = username;
        this.passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());  // ← Secure hash
        this.phoneNumber = phoneNumber;
        this.role = role;
    }

    // For loading from DB
    protected User(String userId, String username, String passwordHash, String phoneNumber, Role role) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.phoneNumber = phoneNumber;
        this.role = role;
    }

    // Check password using BCrypt
    public boolean checkPassword(String plainPassword) {
        if (plainPassword == null || this.passwordHash == null) {
            return false;
        }
        return BCrypt.checkpw(plainPassword, this.passwordHash);
    }

    // Getters
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public Role getRole() { return role; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getPasswordHash() {
        return passwordHash;
    }
    public void persist() {
        new UserDAO().saveUser(this);
    }

    @Override
    public String toString() {
        return String.format("%s [ID: %s, User: %s, Phone: %s, Role: %s]",
                getClass().getSimpleName(), userId, username, phoneNumber, role.getDisplayName());
    }
}