// src/main/java/com/bankingSystem/Service/BankingService.java
package com.bankingSystem.Proxy;

import com.bankingSystem.Account.Account;
import com.bankingSystem.user.User;
import com.bankingSystem.Transaction.Transaction;

import java.util.List;

public interface BankingService {
    // Customer permissions
    void performTransaction(String ownerId, Account from, Account to, double amount, String type);
    List<Transaction> getTransactionHistory(String ownerId, String accountNumber);
    void modifyAccountInfo(String ownerId, Account account, String field, String newValue);

    // Teller permissions
    Account createAccount(String accountType, String ownerId);
    void changeAccountState(String accountNumber, String newState); // e.g., "Frozen"
    Account findAccountByNumber(String accountNumber);

    // Manager permissions
    List<User> getAllTellers();
    void addTeller(String username, String password, String phoneNumber);    void deleteTeller(String userId);
    List<User> getAllCustomers();
    List<Transaction> getAllTransactionsReport();
    List<Account> getAllAccountsReport();
}