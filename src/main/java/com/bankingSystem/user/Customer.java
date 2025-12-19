// Example for Customer.java (same for Teller, Manager)
package com.bankingSystem.user;

public class Customer extends User {
    public Customer(String username, String password, String phoneNumber) {
        super(username, password, phoneNumber, Role.CUSTOMER);
    }

    public Customer(String userId, String username, String passwordHash, String phoneNumber) {
        super(userId, username, passwordHash, phoneNumber, Role.CUSTOMER);
    }
}