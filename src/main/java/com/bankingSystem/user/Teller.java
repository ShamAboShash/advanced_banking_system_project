// src/main/java/com/bankingSystem/user/Teller.java
package com.bankingSystem.user;

public class Teller extends User {
    public Teller(String username, String password, String phoneNumber) {
        super(username, password, phoneNumber, Role.TELLER);  // ← CORRECT ORDER
    }

    public Teller(String userId, String username, String passwordHash, String phoneNumber) {
        super(userId, username, passwordHash, phoneNumber, Role.TELLER);
    }
}