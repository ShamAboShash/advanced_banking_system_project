// src/main/java/com/bankingSystem/Service/RoleBasedAccessProxy.java
package com.bankingSystem.Proxy;

import com.bankingSystem.Account.Account;
import com.bankingSystem.user.*;
import com.bankingSystem.Transaction.Transaction;

import java.util.List;

public class RoleBasedAccessProxy implements BankingService {
    private final BankingServiceImpl realService = new BankingServiceImpl();
    private final User currentUser;

    public RoleBasedAccessProxy(User currentUser) {
        this.currentUser = currentUser;
    }

    private void requireRole(Role requiredRole) {
        if (currentUser.getRole() != requiredRole && currentUser.getRole() != Role.ADMIN) {
            throw new SecurityException("Access denied: Requires " + requiredRole.getDisplayName() + " role");
        }
    }

    @Override
    public void performTransaction(String ownerId, Account from, Account to, double amount, String type) {
        if (currentUser.getRole() == Role.CUSTOMER) {
            if (!currentUser.getUserId().equals(ownerId)) {
                throw new SecurityException("Customers can only perform transactions on their own accounts");
            }
        }
        realService.performTransaction(ownerId, from, to, amount, type);
    }

    @Override
    public List<Transaction> getTransactionHistory(String ownerId, String accountNumber) {
        if (currentUser.getRole() == Role.CUSTOMER && !currentUser.getUserId().equals(ownerId)) {
            throw new SecurityException("Customers can only view their own transaction history");
        }
        return realService.getTransactionHistory(ownerId, accountNumber);
    }

    @Override
    public void modifyAccountInfo(String ownerId, Account account, String field, String newValue) {
        if (currentUser.getRole() == Role.CUSTOMER && !currentUser.getUserId().equals(ownerId)) {
            throw new SecurityException("Customers can only modify their own accounts");
        }
        realService.modifyAccountInfo(ownerId, account, field, newValue);
    }

    @Override
    public Account createAccount(String accountType, String ownerId) {
        requireRole(Role.TELLER);
        return realService.createAccount(accountType, ownerId);
    }

    @Override
    public void changeAccountState(String accountNumber, String newState) {
        requireRole(Role.TELLER);
        realService.changeAccountState(accountNumber, newState);
    }

    @Override
    public Account findAccountByNumber(String accountNumber) {
        requireRole(Role.TELLER);
        return realService.findAccountByNumber(accountNumber);
    }

    @Override
    public List<User> getAllTellers() {
        requireRole(Role.MANAGER);
        return realService.getAllTellers();
    }

    // ← FIXED: Added override with phone number
    @Override
    public void addTeller(String username, String password, String phoneNumber) {
        requireRole(Role.MANAGER);
        realService.addTeller(username, password, phoneNumber);
    }

    @Override
    public void deleteTeller(String userId) {
        requireRole(Role.MANAGER);
        realService.deleteTeller(userId);
    }

    @Override
    public List<User> getAllCustomers() {
        requireRole(Role.MANAGER);
        return realService.getAllCustomers();
    }

    @Override
    public List<Transaction> getAllTransactionsReport() {
        requireRole(Role.MANAGER);
        return realService.getAllTransactionsReport();
    }

    @Override
    public List<Account> getAllAccountsReport() {
        requireRole(Role.MANAGER);
        return realService.getAllAccountsReport();
    }
}