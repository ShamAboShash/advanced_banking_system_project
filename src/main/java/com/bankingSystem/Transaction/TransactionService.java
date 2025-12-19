package com.bankingSystem.Transaction;

import com.bankingSystem.Account.Account;
import com.bankingSystem.Database.AccountDAO;
import com.bankingSystem.Database.TransactionDAO;
import com.bankingSystem.Notification.*;
import com.bankingSystem.Transaction.ChainOfResponsability.*;
import com.bankingSystem.user.Role;

import java.util.ArrayList;
import java.util.List;

public class TransactionService implements Subject {

    // ✅ Singleton
    private static final TransactionService INSTANCE = new TransactionService();

    public static TransactionService getInstance() {
        return INSTANCE;
    }

    private ApprovalHandler approvalChain;
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final AccountDAO accountDAO = new AccountDAO();

    private final List<Observer> observers = new ArrayList<>();

    // ✅ constructor صار private
    public TransactionService() {
        buildApprovalChain();
    }

    private void buildApprovalChain() {
        ApprovalHandler small = new SmallTransactionHandler();
        ApprovalHandler large = new LargeTransactionHandler();
        ApprovalHandler manager = new ManagerApprovalHandler();

        small.setNext(large);
        large.setNext(manager);
        approvalChain = small;
    }

    @Override
    public void registerObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(NotificationEvent event) {
        for (Observer o : observers) {
            if (o.supports(event.getTargetRole())) {
                o.update(event);
            }
        }
    }

    // ===== Manager decision event (approved / rejected) =====
    public void notifyManagerDecision(Transaction tx, boolean approved) {
        if (approved) {
            notifyObservers(new NotificationEvent(
                    NotificationEventType.TRANSACTION_APPROVED_BY_MANAGER,
                    "Manager approved transaction (" + tx.getType() + ", " + tx.getAmount() + ")",
                    tx.getTransactionId(),
                    Role.CUSTOMER
            ));
        } else {
            notifyObservers(new NotificationEvent(
                    NotificationEventType.TRANSACTION_REJECTED_BY_MANAGER,
                    "Manager rejected transaction (" + tx.getType() + ", " + tx.getAmount() + ")",
                    tx.getTransactionId(),
                    Role.CUSTOMER
            ));
        }

        // optional: confirm to manager
        notifyObservers(new NotificationEvent(
                approved ? NotificationEventType.TRANSACTION_APPROVED_BY_MANAGER
                        : NotificationEventType.TRANSACTION_REJECTED_BY_MANAGER,
                "Decision recorded for TxID: " + tx.getTransactionId(),
                tx.getTransactionId(),
                Role.MANAGER
        ));
    }

    public void processTransaction(Account from, Account to, double amount, String type) {
        Transaction tx = new Transaction(type, amount,
                from != null ? from.getAccountNumber() : null,
                to != null ? to.getAccountNumber() : null);

        approvalChain.handle(tx);

        if (tx.isApproved()) {
            // execute
            switch (type) {
                case "DEPOSIT" -> to.deposit(amount);
                case "WITHDRAW" -> from.withdraw(amount);
                case "TRANSFER" -> { from.withdraw(amount); to.deposit(amount); }
                default -> throw new IllegalArgumentException("Unknown transaction type: " + type);
            }

            tx.setStatus("COMPLETED");

            if (from != null) from.addTransaction(tx);
            if (to != null) to.addTransaction(tx);

            if (from != null) from.persist();
            if (to != null) to.persist();

            // notify customer: completed
            notifyObservers(new NotificationEvent(
                    NotificationEventType.TRANSACTION_COMPLETED,
                    "Transaction completed successfully (" + type + ", " + amount + ")",
                    tx.getTransactionId(),
                    Role.CUSTOMER
            ));

        } else {
            // Make pending explicit (حتى ما نضيع بين statuses)
            tx.setStatus("PENDING_MANAGER_APPROVAL");

            // notify manager: pending approval
            notifyObservers(new NotificationEvent(
                    NotificationEventType.TRANSACTION_PENDING_MANAGER_APPROVAL,
                    "Transaction requires manager approval (Amount: " + amount + ")",
                    tx.getTransactionId(),
                    Role.MANAGER
            ));
        }

        // always save for audit + pending workflow
        transactionDAO.saveTransaction(tx);
    }

    public void executeApprovedTransaction(String transactionId) {
        Transaction tx = transactionDAO.loadTransactionById(transactionId);

        if (tx == null || !"APPROVED_BY_MANAGER".equals(tx.getStatus())) {
            System.out.println("Cannot execute: transaction not found or not approved.");
            return;
        }

        Account from = tx.getFromAccount() != null ? accountDAO.loadAccount(tx.getFromAccount()) : null;
        Account to = tx.getToAccount() != null ? accountDAO.loadAccount(tx.getToAccount()) : null;

        try {
            switch (tx.getType()) {
                case "DEPOSIT" -> to.deposit(tx.getAmount());
                case "WITHDRAW" -> from.withdraw(tx.getAmount());
                case "TRANSFER" -> { from.withdraw(tx.getAmount()); to.deposit(tx.getAmount()); }
            }

            transactionDAO.updateTransactionStatus(transactionId, "COMPLETED");

            notifyObservers(new NotificationEvent(
                    NotificationEventType.TRANSACTION_COMPLETED,
                    "Transaction executed successfully after manager approval.",
                    tx.getTransactionId(),
                    Role.CUSTOMER
            ));

        } catch (Exception e) {
            transactionDAO.updateTransactionStatus(transactionId, "FAILED: " + e.getMessage());

            notifyObservers(new NotificationEvent(
                    NotificationEventType.TRANSACTION_FAILED,
                    "Transaction execution failed: " + e.getMessage(),
                    tx.getTransactionId(),
                    Role.CUSTOMER
            ));
        }
    }
}


//package com.bankingSystem.Transaction;
//
//import com.bankingSystem.Account.Account;
//import com.bankingSystem.Database.TransactionDAO;
//import com.bankingSystem.Transaction.ChainOfResponsability.ApprovalHandler;
//import com.bankingSystem.Transaction.ChainOfResponsability.LargeTransactionHandler;
//import com.bankingSystem.Transaction.ChainOfResponsability.ManagerApprovalHandler;
//import com.bankingSystem.Transaction.ChainOfResponsability.SmallTransactionHandler;
//
//public class TransactionService {
//
//    private  ApprovalHandler approvalChain;
//    private  TransactionDAO transactionDAO; // ← جديد: لحفظ المعاملات في الـ DB
//
//    public TransactionService() {
//        this.transactionDAO = new TransactionDAO(); // إنشاء مرة واحدة
//        buildApprovalChain();
//    }
//
//    private void buildApprovalChain() {
//        ApprovalHandler small = new SmallTransactionHandler();
//        ApprovalHandler large = new LargeTransactionHandler();
//        ApprovalHandler manager = new ManagerApprovalHandler();
//
//        small.setNext(large);
//        large.setNext(manager);
//        this.approvalChain = small;
//    }
//
//
//    public void processTransaction(Account from, Account to, double amount, String type) {
//        // إنشاء المعاملة
//        Transaction tx = new Transaction(
//                type,
//                amount,
//                from != null ? from.getAccountNumber() : null,
//                to != null ? to.getAccountNumber() : null
//        );
//
//        // تطبيق سلسلة الموافقة
//        approvalChain.handle(tx);
//
//        // إذا تمت الموافقة → نفذ العملية
//        if (tx.isApproved()) {
//            executeOperation(tx, from, to);
//            tx.setStatus("COMPLETED");
//
//            // حفظ المعاملة في قاعدة البيانات أولاً
//            transactionDAO.saveTransaction(tx);
//
//            // إضافة المعاملة إلى تاريخ الحسابات + حفظ الحسابات في الـ DB
//            if (from != null) {
//                from.addTransaction(tx);
//                from.persist();  // ← يستدعي AccountDAO.saveAccount(this)
//            }
//            if (to != null) {
//                to.addTransaction(tx);
//                to.persist();    // ← يستدعي AccountDAO.saveAccount(this)
//            }
//
//            System.out.println("Transaction COMPLETED & PERSISTED: " + tx);
//        }
//        // إذا كانت تنتظر موافقة المدير
//        else if (tx.isPendingManagerApproval()) {
//            transactionDAO.saveTransaction(tx); // ← نحفظها حتى لو معلقة
//            System.out.println("Transaction PENDING MANAGER APPROVAL → Saved with ID: " + tx.getTransactionId());
//        }
//        // إذا تم رفضها
//        else {
//            transactionDAO.saveTransaction(tx); // اختياري: نحفظ حتى المرفوضة للـ audit
//            System.out.println("Transaction REJECTED: " + tx.getStatus());
//        }
//    }
//
//    private void executeOperation(Transaction tx, Account from, Account to) {
//        switch (tx.getType()) {
//            case "DEPOSIT" -> to.deposit(tx.getAmount());
//            case "WITHDRAW" -> from.withdraw(tx.getAmount());
//            case "TRANSFER" -> {
//                from.withdraw(tx.getAmount());
//                to.deposit(tx.getAmount());
//            }
//            default -> throw new IllegalArgumentException("Unknown transaction type: " + tx.getType());
//        }
//    }

    // === طريقة إضافية للموافقة اليدوية من المدير (تُستدعى من Bank) ===
//    public void approvePendingTransaction(String transactionId, boolean approve) {
//        Transaction tx = transactionDAO.loadTransactions(null).stream()
//                .filter(t -> t.getTransactionId().equals(transactionId))
//                .findFirst()
//                .orElse(null);
//        if (tx == null || !tx.isPendingManagerApproval()) {
//            System.out.println("Transaction not found or not pending approval.");
//            return;
//        }
//        if (approve) {
//            tx.setStatus("APPROVED");
//            S from = tx.getFromAccount() ;
//            Account to = tx.getToAccount() != null ? Bank.getInstance().getAccount(tx.getToAccount()) : null;
//            executeOperation(tx, from, to);
//            tx.setStatus("COMPLETED");
//
//            if (from != null) { from.addTransaction(tx); from.persist(); }
//            if (to != null)   { to.addTransaction(tx);   to.persist(); }
//
//            transactionDAO.saveTransaction(tx); // تحديث الحالة في الـ DB
//            System.out.println("Manager APPROVED & EXECUTED transaction: " + transactionId);
//        } else {
//            tx.setStatus("REJECTED_BY_MANAGER");
//            transactionDAO.saveTransaction(tx);
//            System.out.println("Manager REJECTED transaction: " + transactionId);
//        }
//    }
//}