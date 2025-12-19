// src/main/java/com/bankingSystem/Account/Account.java
package com.bankingSystem.Account;

import com.bankingSystem.Account.CompositePattern.AccountComponent;
import com.bankingSystem.Account.statePattern.AccountState;
import com.bankingSystem.Account.statePattern.ActiveState;
import com.bankingSystem.Interest.InterestStrategy;
import com.bankingSystem.Transaction.Transaction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class Account implements AccountComponent {
    public String accountId;
    public String accountNumber;
    public double balance;
    public LocalDateTime creationDate;
    public String ownerId;
    public AccountState state;
    public InterestStrategy interestStrategy;
    public List<Transaction> transactionHistory = new ArrayList<>();

    public Account(String accountNumber, String ownerId) {
        this.accountId = UUID.randomUUID().toString();
        this.accountNumber = accountNumber;
        this.ownerId = ownerId;
        this.balance = 0.0;
        this.creationDate = LocalDateTime.now();
        this.state = new ActiveState();
    }

    // Abstract method - must be overridden in subclasses to return double
    public abstract double calculateInterest();

    // State Pattern methods
    public void deposit(double amount) {
        state.deposit(this, amount);
    }

    public void withdraw(double amount) {
        state.withdraw(this, amount);
    }

    public void freeze() { state.freeze(this); }
    public void suspend() { state.suspend(this); }
    public void activate() { state.activate(this); }
    public void close() { state.close(this); }

    // Transaction methods
    public void addTransaction(Transaction transaction) {
        transactionHistory.add(transaction);
    }

    // Observer stub (for Notification - can be extended later for full Observer Pattern)
    public void notifyObservers(String message) {
        System.out.println("Notification: " + message);
    }

    // Getters/Setters
    public String getAccountNumber() { return accountNumber; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public void setState(AccountState state) { this.state = state; }
    public List<Transaction> getTransactionHistory() { return new ArrayList<>(transactionHistory); }
    public void setInterestStrategy(InterestStrategy strategy) { this.interestStrategy = strategy; }
    public String getOwnerId() { return ownerId; }

    // ←←←←← NEW METHOD TO FIX THE COMPILATION ERRORS ←←←←←
    /**
     * Returns the account type as a clean string (e.g., "SavingsAccount", "CheckingAccount")
     * Used in manager reports and console display
     */
    public String getType() {
        String fullName = this.getClass().getSimpleName();
        // Remove "Account" suffix if present (e.g., SavingsAccount → Savings)
        if (fullName.endsWith("Account")) {
            return fullName.substring(0, fullName.length() - 7);
        }
        return fullName;
    }

    // Composite methods
    public void showDetails() {
        System.out.printf("%s | %s | Balance: %.2f | Status: %s%n",
                getType(), // ← Now uses getType() instead of getClass().getSimpleName()
                accountNumber, balance,
                state.getClass().getSimpleName().replace("State", ""));
    }

    public double getTotalBalance() { return balance; }

    @Override
    public String toString() {
        return getType() + "[" + accountNumber + "] Balance: " + balance;
    }

    public void persist() {
        new com.bankingSystem.Database.AccountDAO().saveAccount(this);
    }

    public void modify(String field, String newValue) {
        switch (field.toLowerCase()) {
            case "owner" -> {
                String old = this.ownerId;
                this.ownerId = newValue;
                System.out.println("Owner changed: " + old + " → " + newValue);
            }
            case "overdraftlimit" -> {
                if (this instanceof CheckingAccount chk) {
                    try {
                        var f = CheckingAccount.class.getDeclaredField("overdraftLimit");
                        f.setAccessible(true);
                        f.setDouble(chk, Double.parseDouble(newValue));
                        System.out.println("Overdraft limit updated to: " + newValue);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to update overdraft limit", e);
                    }
                } else {
                    throw new IllegalStateException("Only CheckingAccount has overdraft limit");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported field: " + field);
        }

        this.persist();
        System.out.println("Account modified and saved: " + getAccountNumber());
    }

    public void updateOwner(String newOwnerId) {
        if (newOwnerId == null || newOwnerId.trim().isEmpty()) {
            throw new IllegalArgumentException("New owner ID cannot be empty");
        }
        String oldOwnerId = this.ownerId;
        this.ownerId = newOwnerId;

        System.out.println("Account ownership changed: " + oldOwnerId + " → " + newOwnerId
                + " | Account: " + getAccountNumber()
                + " | Time: " + LocalDateTime.now());

        persist();
    }
}