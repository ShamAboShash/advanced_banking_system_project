package com.bankingSystem.user;

import com.bankingSystem.Database.AccountDAO;
import com.bankingSystem.Database.TransactionDAO;
import com.bankingSystem.Transaction.Transaction;
import com.bankingSystem.Transaction.TransactionService;
import com.bankingSystem.Account.Account;

public class Manager extends User {

    public Manager(String username, String password, String phoneNumber) {
        super(username, password, phoneNumber, Role.MANAGER);
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

        // Resolve customerId (from/to account owner)
        AccountDAO accountDAO = new AccountDAO();
        Account from = tx.getFromAccount() != null ? accountDAO.loadAccount(tx.getFromAccount()) : null;
        Account to = tx.getToAccount() != null ? accountDAO.loadAccount(tx.getToAccount()) : null;
        String customerId = (from != null) ? from.ownerId : (to != null ? to.ownerId : null);

        TransactionService service = TransactionService.getInstance();
        service.notifyManagerDecision(tx, true, customerId, this.userId);
        service.executeApprovedTransaction(transactionId);

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

        // Resolve customerId
        AccountDAO accountDAO = new AccountDAO();
        Account from = tx.getFromAccount() != null ? accountDAO.loadAccount(tx.getFromAccount()) : null;
        Account to = tx.getToAccount() != null ? accountDAO.loadAccount(tx.getToAccount()) : null;
        String customerId = (from != null) ? from.ownerId : (to != null ? to.ownerId : null);

        TransactionService service = TransactionService.getInstance();
        service.notifyManagerDecision(tx, false, customerId, this.userId);

        System.out.println("Manager " + username + " rejected transaction " + transactionId);
    }
}
