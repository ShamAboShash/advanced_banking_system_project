// src/main/java/com/bankingSystem/user/Manager.java
package com.bankingSystem.user;

import com.bankingSystem.Database.TransactionDAO;
import com.bankingSystem.Transaction.Transaction;
import com.bankingSystem.Transaction.TransactionService;

public class Manager extends User {
    public Manager(String username, String password) {
        super(username, password, Role.MANAGER);
    }

    public Manager(String userId, String username, String passwordHash) {
        super(userId, username, passwordHash, Role.MANAGER);
    }

    public void approveTransaction(String transactionId) {
        TransactionDAO txDAO = new TransactionDAO();
        Transaction tx = txDAO.loadTransactionById(transactionId);

        if (tx == null) {
            System.out.println("Transaction not found: " + transactionId);
            return;
        }

        txDAO.updateTransactionStatus(transactionId, "APPROVED_BY_MANAGER");

        TransactionService service = new TransactionService();
        service.notifyManagerDecision(tx, true);            // ✅ إشعار Approved
        service.executeApprovedTransaction(transactionId);  // ✅ التنفيذ

        System.out.println("Manager " + username + " approved and executed transaction " + transactionId);
    }

    public void rejectTransaction(String transactionId) {
        TransactionDAO txDAO = new TransactionDAO();
        Transaction tx = txDAO.loadTransactionById(transactionId);

        if (tx == null) {
            System.out.println("Transaction not found: " + transactionId);
            return;
        }

        txDAO.updateTransactionStatus(transactionId, "REJECTED_BY_MANAGER");

        TransactionService service = new TransactionService();
        service.notifyManagerDecision(tx, false); // ✅ إشعار Rejected

        System.out.println("Manager " + username + " rejected transaction " + transactionId);
    }
}
