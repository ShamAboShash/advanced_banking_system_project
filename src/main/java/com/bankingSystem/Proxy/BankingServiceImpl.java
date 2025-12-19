// src/main/java/com/bankingSystem/Service/BankingServiceImpl.java
package com.bankingSystem.Proxy;

import com.bankingSystem.Account.*;
import com.bankingSystem.Database.*;
import com.bankingSystem.user.*;
import com.bankingSystem.Transaction.*;

import java.util.List;

public class BankingServiceImpl implements BankingService {
    private static TransactionService transactionService = TransactionService.getInstance();
    private final AccountDAO accountDAO = new AccountDAO();
    private final UserDAO userDAO = new UserDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    @Override
    public void performTransaction(String ownerId, Account from, Account to, double amount, String type) {
        transactionService.processTransaction(from, to, amount, type);
    }

    @Override
    public List<Transaction> getTransactionHistory(String ownerId, String accountNumber) {
        Account account = accountDAO.loadAccount(accountNumber);
        if (account != null && account.getOwnerId().equals(ownerId)) {
            return account.getTransactionHistory();
        }
        throw new SecurityException("Access denied: Not your account");
    }

    @Override
    public void modifyAccountInfo(String ownerId, Account account, String field, String newValue) {
        if (account.getOwnerId().equals(ownerId)) {
            account.modify(field, newValue);
        } else {
            throw new SecurityException("Cannot modify another's account");
        }
    }

    @Override
    public Account createAccount(String accountType, String ownerId) {
        Account account;
        switch (accountType.toLowerCase()) {
            case "savings" -> account = new SavingsAccount(generateShortId("SAV"), ownerId);
            case "checking" -> account = new CheckingAccount(generateShortId("CHK"), ownerId);
            case "loan" -> account = new LoanAccount(generateShortId("LOAN"), ownerId, 0.0);
            case "investment" -> account = new InvestmentAccount(generateShortId("INV"), ownerId);
            default -> throw new IllegalArgumentException("Unknown account type");
        }
        account.persist();
        return account;
    }

    // ← NEW: Generate short IDs like SAV-001
    private static int counter = 1;
    private String generateShortId(String prefix) {
        return String.format("%s-%03d", prefix, counter++);
    }

    @Override
    public void changeAccountState(String accountNumber, String newState) {
        Account account = accountDAO.loadAccount(accountNumber);
        if (account != null) {
            switch (newState.toLowerCase()) {
                case "active" -> account.activate();
                case "frozen" -> account.freeze();
                case "suspended" -> account.suspend();
                case "closed" -> account.close();
            }
            account.persist();
        }
    }

    @Override
    public Account findAccountByNumber(String accountNumber) {
        return accountDAO.loadAccount(accountNumber);
    }

    @Override
    public List<User> getAllTellers() {
        return userDAO.getAllUsersByRole("Teller");
    }

    // ← FIXED: Added phone number
    @Override
    public void addTeller(String username, String password, String phoneNumber) {
        Teller teller = new Teller(username, password, phoneNumber);
        teller.persist();
    }

    @Override
    public void deleteTeller(String userId) {
        userDAO.deleteUser(userId);
    }

    @Override
    public List<User> getAllCustomers() {
        return userDAO.getAllUsersByRole("Customer");
    }

    @Override
    public List<Transaction> getAllTransactionsReport() {
        return transactionDAO.loadAllTransactions();
    }

    @Override
    public List<Account> getAllAccountsReport() {
        return accountDAO.getAllAccounts();
    }
}