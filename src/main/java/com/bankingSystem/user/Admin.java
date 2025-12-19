// src/main/java/com/bankingSystem/Roles/Admin.java
package com.bankingSystem.user;

public class Admin extends User {
    public Admin(String username, String phonNymber ,String password) {
        super(username, password, phonNymber ,Role.ADMIN);
    }

    public Admin(String userId, String username, String phoneNumber,String passwordHash) {
        super(userId, username, passwordHash, phoneNumber,Role.ADMIN);
    }

    // Admins can manage users, view all reports, etc.
}