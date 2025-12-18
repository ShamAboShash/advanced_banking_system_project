// src/main/java/com/bankingSystem/BankingSystemDemo.java
package com.bankingSystem;

import com.bankingSystem.Account.*;
import com.bankingSystem.Account.CompositePattern.AccountGroup;
import com.bankingSystem.Database.*;
import com.bankingSystem.Transaction.Transaction;
import com.bankingSystem.user.*;
import com.bankingSystem.Transaction.TransactionService;

import java.util.List;

public class CoreApplication {

    private static TransactionService transactionService = new TransactionService();
    private static AccountDAO accountDAO = new AccountDAO();
    private static UserDAO userDAO = new UserDAO();
    private static TransactionDAO transactionDAO = new TransactionDAO();


    public static void main(String[] args) {
        transactionService.registerObserver(new com.bankingSystem.Notification.EmailNotifier(com.bankingSystem.user.Role.CUSTOMER));
        transactionService.registerObserver(new com.bankingSystem.Notification.SMSNotifier(com.bankingSystem.user.Role.CUSTOMER));
        transactionService.registerObserver(new com.bankingSystem.Notification.InAppNotifier(com.bankingSystem.user.Role.CUSTOMER));

        transactionService.registerObserver(new com.bankingSystem.Notification.EmailNotifier(com.bankingSystem.user.Role.MANAGER));
        transactionService.registerObserver(new com.bankingSystem.Notification.SMSNotifier(com.bankingSystem.user.Role.MANAGER));
        transactionService.registerObserver(new com.bankingSystem.Notification.InAppNotifier(com.bankingSystem.user.Role.MANAGER));

        System.out.println("=== Advanced Banking System Demo ===\n");
        System.out.println("Phase 1: Creating Users (Customer, Teller, Manager)\n");

        Customer customer = new Customer("ahmed.ali", "pass123");
        Teller teller = new Teller("teller_sara", "tellerpass");
        Manager manager = new Manager("mgr_khaled", "mgrpass");

        customer.persist();
        teller.persist();
        manager.persist();

        System.out.println("Users created and saved:\n" + customer + "\n" + teller + "\n" + manager + "\n");

        // ===================================================================
        System.out.println("Phase 2: Creating Accounts for Ahmed\n");

        SavingsAccount savings = new SavingsAccount("SAV-1001", customer.getUserId());
        CheckingAccount checking = new CheckingAccount("CHK-2001", customer.getUserId());
        LoanAccount loan = new LoanAccount("LOAN-3001", customer.getUserId(), 50000.0);
        InvestmentAccount investment = new InvestmentAccount("INV-4001", customer.getUserId());

        savings.persist();
        checking.persist();
        loan.persist();
        investment.persist();

        // Composite Pattern: Family Group Account
        AccountGroup familyGroup = new AccountGroup("Ahmed's Family Accounts");
        familyGroup.add(savings);
        familyGroup.add(checking);
        familyGroup.add(investment);

        System.out.println("Accounts created and grouped:");
        familyGroup.showDetails();
        System.out.println();

        // ===================================================================
        System.out.println("Phase 3: Normal Operations (Deposits & Small Transfers)\n");

        transactionService.processTransaction(null, savings, 15000.0, "DEPOSIT");     // Deposit 15k
        transactionService.processTransaction(null, savings, 15000.0, "DEPOSIT");     // Deposit 15k
        transactionService.processTransaction(null, savings, 15000.0, "DEPOSIT");     // Deposit 15k
        transactionService.processTransaction(null, savings, 15000.0, "DEPOSIT");     // Deposit 15k
        transactionService.processTransaction(null, savings, 15000.0, "DEPOSIT");     // Deposit 15k
        transactionService.processTransaction(null, savings, 15000.0, "DEPOSIT");     // Deposit 15k
        transactionService.processTransaction(null, checking, 8000.0, "DEPOSIT");     // Deposit 8k
        transactionService.processTransaction(checking, savings, 3000.0, "TRANSFER");       // Small transfer
        System.out.println("\nSavings after operations:");
        savings.showDetails();
        System.out.println("Checking after operations:");
        checking.showDetails();

        // ===================================================================
        System.out.println("\nPhase 4: Large Transfer (>50,000) → Requires Manager Approval\n");

        transactionService.processTransaction(savings, checking, 60000.0, "TRANSFER");
        // This will be PENDING - money NOT moved yet

        System.out.println("\nCurrent balances (money not moved yet):");
        savings.showDetails();
        checking.showDetails();

        // ===================================================================

        System.out.println("\n=== TEST: Manager Rejects Large Transaction ===");

        List<Transaction> pendingReject = transactionDAO.loadPendingTransactions();
        if (!pendingReject.isEmpty()) {
            Transaction txToReject = pendingReject.get(0);

            System.out.println("Rejecting transaction:");
            System.out.println("  → " + txToReject);

            Manager mgr = (Manager) userDAO.loadUser("mgr_khaled");
            mgr.rejectTransaction(txToReject.getTransactionId());

            System.out.println("Balances after rejection (should be unchanged):");
            reloadAndShowAccount(savings);
            reloadAndShowAccount(checking);
        }

        // ===================================================================
        System.out.println("\nPhase 5: Manager Approves Large Transaction\n");

        User loadedManager = userDAO.loadUser("mgr_khaled");
        if (loadedManager instanceof Manager mgr) {
            List<Transaction> pending = transactionDAO.loadPendingTransactions();
            if (!pending.isEmpty()) {
                Transaction pendingTx = pending.get(pending.size() - 1); // Latest one
                System.out.println("Found pending transaction:");
                System.out.println("  → " + pendingTx);

                mgr.approveTransaction(pendingTx.getTransactionId());

                System.out.println("\nFinal balances after approval:");
                reloadAndShowAccount(savings);
                reloadAndShowAccount(checking);
            } else {
                System.out.println("No pending transactions found (did you run a large transfer?)");
            }
        }
        // ===================================================================
        System.out.println("\nPhase 6: State Pattern Demo (Freeze & Close Account)\n");

        System.out.println("Freezing Savings Account...");
        savings.freeze();
        savings.persist();

        try {
            transactionService.processTransaction(null, savings, 1000.0, "DEPOSIT"); // Allowed in Frozen
            transactionService.processTransaction(savings, checking, 500.0, "TRANSFER"); // Blocked
        } catch (Exception e) {
            System.out.println("Expected error: " + e.getMessage());
        }

        System.out.println("\nClosing Loan Account...");
        loan.close();
        loan.persist();

        try {
            loan.makePayment(1000);
        } catch (Exception e) {
            System.out.println("Expected error on closed account: " + e.getMessage());
        }

        // ===================================================================
        System.out.println("\nPhase 7: Interest Calculation (Strategy Pattern)\n");

        System.out.printf("Savings Interest (4%%): %.2f%n", savings.calculateInterest());
        System.out.printf("Loan Interest (monthly): %.2f%n", loan.calculateInterest());
        System.out.printf("Investment Interest (volatile): %.2f%n", investment.calculateInterest());

        // ===================================================================
        System.out.println("\nPhase 8: Simulate Application Restart – Reload from DB\n");

        System.out.println("Reloading all accounts for Ahmed from database...");
        List<Account> reloadedAccounts = accountDAO.getAccountsByOwner(customer.getUserId());

        for (Account acc : reloadedAccounts) {
            acc.showDetails();
            System.out.println("Transaction History (" + acc.getTransactionHistory().size() + " entries):");
            for (Transaction tx : acc.getTransactionHistory()) {
                System.out.println("  → " + tx);
            }
            System.out.println();
        }

        System.out.println("\n=== Demo Completed Successfully! ===");
        System.out.println("You have demonstrated:");
        System.out.println("   • Composite, State, Strategy, Chain of Responsibility");
        System.out.println("   • Full persistence (H2 DB)");
        System.out.println("   • Role-based users & manager approval workflow");
        System.out.println("   • Transaction history & audit trail");
        System.out.println("   • Account states and restrictions");
    }

    private static void reloadAndShowAccount(Account account) {
        Account fresh = accountDAO.loadAccount(account.getAccountNumber());
        if (fresh != null) {
            fresh.showDetails();
        }
    }
}