// src/main/java/com/bankingSystem/user/Manager.java
package com.bankingSystem.user;

import com.bankingSystem.Database.TransactionDAO;
import com.bankingSystem.Transaction.Transaction;
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
        Transaction tx = txDAO.loadTransactionById(transactionId);

        if (tx == null) {
            System.out.println("Transaction not found: " + transactionId);
            return;
        }

        txDAO.updateTransactionStatus(transactionId, "APPROVED_BY_MANAGER");


        // ✅ نفس instance
        TransactionService service = TransactionService.getInstance();
        service.notifyManagerDecision(tx, true);
        service.executeApprovedTransaction(transactionId);

        new TransactionService().executeApprovedTransaction(transactionId);
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

        // ✅ نفس instance
        TransactionService service = TransactionService.getInstance();
        service.notifyManagerDecision(tx, false);

        System.out.println("Manager " + username + " rejected transaction " + transactionId);
    }
}
