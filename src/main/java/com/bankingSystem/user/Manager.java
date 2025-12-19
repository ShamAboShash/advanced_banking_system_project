// src/main/java/com/bankingSystem/user/Manager.java
package com.bankingSystem.user;

import com.bankingSystem.Database.TransactionDAO;
import com.bankingSystem.Transaction.TransactionService;

public class Manager extends User {
    public Manager(String username, String password, String phoneNumber) {
        super(username, password, phoneNumber, Role.MANAGER);  // ← CORRECT ORDER
    }

    public Manager(String userId, String username, String passwordHash, String phoneNumber) {
        super(userId, username, passwordHash, phoneNumber, Role.MANAGER);
    }

    public void approveTransaction(String transactionId) {
        TransactionDAO txDAO = new TransactionDAO();
        txDAO.updateTransactionStatus(transactionId, "APPROVED_BY_MANAGER");
        new TransactionService().executeApprovedTransaction(transactionId);
        System.out.println("Manager " + username + " approved and executed transaction " + transactionId);
    }
}