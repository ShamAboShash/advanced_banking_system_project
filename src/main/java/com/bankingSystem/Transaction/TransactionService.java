package com.bankingSystem.Transaction;

import com.bankingSystem.Account.Account;
import com.bankingSystem.Database.AccountDAO;
import com.bankingSystem.Database.TransactionDAO;
import com.bankingSystem.Database.UserDAO;
import com.bankingSystem.Notification.*;
import com.bankingSystem.Transaction.ChainOfResponsability.*;
import com.bankingSystem.user.Role;
import com.bankingSystem.user.User;



public class TransactionService implements Subject {

    // ✅ Singleton
    private static final TransactionService INSTANCE = new TransactionService();
    public static TransactionService getInstance() { return INSTANCE; }

    private ApprovalHandler approvalChain;
    private TransactionDAO transactionDAO = new TransactionDAO();
    private AccountDAO accountDAO = new AccountDAO();
    private final UserDAO userDAO = new UserDAO();

    // ✅ Use centralized NotificationCenter (recommended)
    private final NotificationCenter center = NotificationCenter.getInstance();

    // ===== Notification thresholds (tweak as you like) =====
    private static final double LARGE_TX_THRESHOLD = 5000.0;
    private static final double LOW_BALANCE_THRESHOLD = 50.0;

    private TransactionService() {
        buildApprovalChain();
    }

    public void setDaosForTests(TransactionDAO txDao, AccountDAO accDao) {
        this.transactionDAO = txDao;
        this.accountDAO = accDao;
    }

    private void buildApprovalChain() {
        ApprovalHandler small = new SmallTransactionHandler();
        ApprovalHandler large = new LargeTransactionHandler();
        ApprovalHandler manager = new ManagerApprovalHandler();

        small.setNext(large);
        large.setNext(manager);
        approvalChain = small;
    }

    private String resolveManagerId() {
        User m = userDAO.loadFirstManager();
        return (m != null) ? m.getUserId() : "default-manager-001";
    }

    // ========= Subject (delegated) =========
    @Override
    public void registerObserver(Observer observer) {
        center.registerObserver(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        center.removeObserver(observer);
    }

    @Override
    public void notifyObservers(NotificationEvent event) {
        center.notifyObservers(event);
    }

    // ========= Notifications =========

    /**
     * Called by Manager after approve/reject.
     * customerId and managerId are resolved already in Manager class.
     */
    public void notifyManagerDecision(Transaction tx, boolean approved, String customerId, String managerId) {

        // notify customer
        if (customerId != null) {
            notifyObservers(new NotificationEvent(
                    approved ? NotificationEventType.TRANSACTION_APPROVED_BY_MANAGER
                            : NotificationEventType.TRANSACTION_REJECTED_BY_MANAGER,
                    approved
                            ? "Manager approved transaction (" + tx.getType() + ", " + tx.getAmount() + ")"
                            : "Manager rejected transaction (" + tx.getType() + ", " + tx.getAmount() + ")",
                    tx.getTransactionId(),
                    Role.CUSTOMER,
                    customerId
            ));
        }

        // confirm to manager
        if (managerId == null) managerId = resolveManagerId();
        notifyObservers(new NotificationEvent(
                approved ? NotificationEventType.TRANSACTION_APPROVED_BY_MANAGER
                        : NotificationEventType.TRANSACTION_REJECTED_BY_MANAGER,
                "Decision recorded for TxID: " + tx.getTransactionId(),
                tx.getTransactionId(),
                Role.MANAGER,
                managerId
        ));
    }

    // ✅ NEW: called from CoreApplication option 9 (Approve/Reject)
    public void managerDecision(String transactionId, boolean approved, String managerId) {

        Transaction tx = transactionDAO.loadTransactionById(transactionId);

        if (tx == null) {
            synchronized (ConsoleLock.PRINT_LOCK) {
                System.out.println("Transaction not found: " + transactionId);
            }
            return;
        }

        // لازم تكون Pending أصلاً
        if (!"PENDING_MANAGER_APPROVAL".equals(tx.getStatus())) {
            synchronized (ConsoleLock.PRINT_LOCK) {
                System.out.println("Transaction is not pending approval: " + tx.getStatus());
            }
            return;
        }

        // resolve customerId (from/to owner)
        Account from = tx.getFromAccount() != null ? accountDAO.loadAccount(tx.getFromAccount()) : null;
        Account to   = tx.getToAccount() != null ? accountDAO.loadAccount(tx.getToAccount()) : null;
        String customerId = (from != null) ? from.ownerId : (to != null ? to.ownerId : null);

        if (managerId == null) managerId = resolveManagerId();

        if (!approved) {
            // 1) update status
            transactionDAO.updateTransactionStatus(transactionId, "REJECTED_BY_MANAGER");

            // 2) notify customer + manager (and store in DB via persistence observer)
            notifyManagerDecision(tx, false, customerId, managerId);

            synchronized (ConsoleLock.PRINT_LOCK) {
                System.out.println("Manager rejected transaction: " + transactionId);
            }
            return;
        }

        // approved
        transactionDAO.updateTransactionStatus(transactionId, "APPROVED_BY_MANAGER");

        // notify customer + manager
        notifyManagerDecision(tx, true, customerId, managerId);

        // execute (will send COMPLETED/FAILED to customer)
        executeApprovedTransaction(transactionId);

        synchronized (ConsoleLock.PRINT_LOCK) {
            System.out.println("Manager approved & executed transaction: " + transactionId);
        }
    }

    // ========= Main flow =========

    public void processTransaction(Account from, Account to, double amount, String type) {
        Transaction tx = new Transaction(
                type,
                amount,
                from != null ? from.getAccountNumber() : null,
                to != null ? to.getAccountNumber() : null
        );

        // Resolve the customer who should receive notifications for this request
        String customerId =
                (from != null) ? from.ownerId :
                        (to != null)   ? to.ownerId : null;

        // (1) ✅ NEW: notify submitted (real-time + DB)
        if (customerId != null) {
            notifyObservers(new NotificationEvent(
                    NotificationEventType.TRANSACTION_SUBMITTED,
                    "Transaction submitted (" + type + ", " + amount + ")",
                    tx.getTransactionId(),
                    Role.CUSTOMER,
                    customerId
            ));
        }

        // (2) ✅ NEW: large transaction alerts (customer + manager)
        if (amount >= LARGE_TX_THRESHOLD) {
            if (customerId != null) {
                notifyObservers(new NotificationEvent(
                        NotificationEventType.LARGE_TRANSACTION_ALERT,
                        "Large transaction detected: " + amount + " (" + type + ")",
                        tx.getTransactionId(),
                        Role.CUSTOMER,
                        customerId
                ));
            }

            notifyObservers(new NotificationEvent(
                    NotificationEventType.LARGE_TRANSACTION_ALERT,
                    "Large transaction requires attention: " + amount + " (" + type + ") TxID=" + tx.getTransactionId(),
                    tx.getTransactionId(),
                    Role.MANAGER,
                    resolveManagerId()
            ));
        }

        // Approval chain (small/large/manager approval)
        approvalChain.handle(tx);

        if (tx.isApproved()) {
            // execute immediately
            try {
                switch (type) {
                    case "DEPOSIT" -> {
                        if (to == null) throw new IllegalStateException("Deposit target account is null.");
                        to.deposit(amount);
                    }
                    case "WITHDRAW" -> {
                        if (from == null) throw new IllegalStateException("Withdraw source account is null.");
                        from.withdraw(amount);
                    }
                    case "TRANSFER" -> {
                        if (from == null || to == null) throw new IllegalStateException("Transfer accounts are null.");
                        from.withdraw(amount);
                        to.deposit(amount);
                    }
                    default -> throw new IllegalArgumentException("Unknown transaction type: " + type);
                }

                tx.setStatus("COMPLETED");

                if (from != null) from.addTransaction(tx);
                if (to != null) to.addTransaction(tx);

                if (from != null) from.persist();
                if (to != null) to.persist();

                // customer notifications (existing behavior, kept)
                if ("DEPOSIT".equals(type) && to != null) {
                    notifyObservers(new NotificationEvent(
                            NotificationEventType.TRANSACTION_COMPLETED,
                            "Deposit completed: +" + amount + " to " + to.getAccountNumber(),
                            tx.getTransactionId(),
                            Role.CUSTOMER,
                            to.ownerId
                    ));

                } else if ("WITHDRAW".equals(type) && from != null) {
                    notifyObservers(new NotificationEvent(
                            NotificationEventType.TRANSACTION_COMPLETED,
                            "Withdraw completed: -" + amount + " from " + from.getAccountNumber(),
                            tx.getTransactionId(),
                            Role.CUSTOMER,
                            from.ownerId
                    ));

                } else if ("TRANSFER".equals(type) && from != null && to != null) {
                    // sender
                    notifyObservers(new NotificationEvent(
                            NotificationEventType.TRANSACTION_COMPLETED,
                            "Transfer sent: -" + amount + " to " + to.getAccountNumber(),
                            tx.getTransactionId(),
                            Role.CUSTOMER,
                            from.ownerId
                    ));
                    // receiver
                    notifyObservers(new NotificationEvent(
                            NotificationEventType.TRANSACTION_COMPLETED,
                            "Transfer received: +" + amount + " from " + from.getAccountNumber(),
                            tx.getTransactionId(),
                            Role.CUSTOMER,
                            to.ownerId
                    ));
                }

                // (3) ✅ NEW: low balance warning (after successful withdraw/transfer)
                if (from != null && ("WITHDRAW".equals(type) || "TRANSFER".equals(type))) {
                    if (from.getBalance() <= LOW_BALANCE_THRESHOLD) {
                        notifyObservers(new NotificationEvent(
                                NotificationEventType.LOW_BALANCE_WARNING,
                                "Low balance warning: " + from.getBalance() + " on account " + from.getAccountNumber(),
                                tx.getTransactionId(),
                                Role.CUSTOMER,
                                from.ownerId
                        ));
                    }
                }

            } catch (Exception e) {
                tx.setStatus("FAILED: " + e.getMessage());

                // (4) ✅ NEW: validation failed event (business/validation cause)
                if (customerId != null) {
                    notifyObservers(new NotificationEvent(
                            NotificationEventType.TRANSACTION_VALIDATION_FAILED,
                            "Validation failed: " + e.getMessage(),
                            tx.getTransactionId(),
                            Role.CUSTOMER,
                            customerId
                    ));
                }

                // existing: notify failed
                if (customerId != null) {
                    notifyObservers(new NotificationEvent(
                            NotificationEventType.TRANSACTION_FAILED,
                            "Transaction failed: " + e.getMessage(),
                            tx.getTransactionId(),
                            Role.CUSTOMER,
                            customerId
                    ));
                }
            }

        } else {
            // pending manager approval
            tx.setStatus("PENDING_MANAGER_APPROVAL");
            String managerId = resolveManagerId();

            notifyObservers(new NotificationEvent(
                    NotificationEventType.TRANSACTION_PENDING_MANAGER_APPROVAL,
                    "Transaction requires manager approval (Amount: " + amount + ") TxID=" + tx.getTransactionId(),
                    tx.getTransactionId(),
                    Role.MANAGER,
                    managerId
            ));

            // (optional) you can also notify the customer that it's pending
            // if (customerId != null) {
            //     notifyObservers(new NotificationEvent(
            //             NotificationEventType.TRANSACTION_PENDING_MANAGER_APPROVAL,
            //             "Your transaction is pending manager approval. TxID=" + tx.getTransactionId(),
            //             tx.getTransactionId(),
            //             Role.CUSTOMER,
            //             customerId
            //     ));
            // }
        }

        // always save for audit + pending workflow
        transactionDAO.saveTransaction(tx);
    }

    public void executeApprovedTransaction(String transactionId) {
        Transaction tx = transactionDAO.loadTransactionById(transactionId);

        if (tx == null || !"APPROVED_BY_MANAGER".equals(tx.getStatus())) {
            synchronized (ConsoleLock.PRINT_LOCK) {
                System.out.println("Cannot execute: transaction not found or not approved.");
            }
            return;
        }

        Account from = tx.getFromAccount() != null ? accountDAO.loadAccount(tx.getFromAccount()) : null;
        Account to   = tx.getToAccount() != null ? accountDAO.loadAccount(tx.getToAccount()) : null;

        String customerId = (from != null) ? from.ownerId : (to != null ? to.ownerId : null);

        try {
            // 1) execute balance changes
            switch (tx.getType()) {
                case "DEPOSIT" -> {
                    if (to == null) throw new IllegalStateException("Deposit target account not found.");
                    to.deposit(tx.getAmount());
                }
                case "WITHDRAW" -> {
                    if (from == null) throw new IllegalStateException("Withdraw source account not found.");
                    from.withdraw(tx.getAmount());
                }
                case "TRANSFER" -> {
                    if (from == null || to == null) throw new IllegalStateException("Transfer accounts not found.");
                    from.withdraw(tx.getAmount());
                    to.deposit(tx.getAmount());
                }
                default -> throw new IllegalArgumentException("Unknown transaction type: " + tx.getType());
            }

            // 2) attach tx to accounts (history)
            if (from != null) from.addTransaction(tx);
            if (to != null) to.addTransaction(tx);

            // 3) persist accounts
            if (from != null) from.persist();
            if (to != null) to.persist();

            // 4) mark executed in DB (status + performedAt)
            transactionDAO.markExecuted(transactionId, "COMPLETED");

            // 5) notify customer
            if (customerId != null) {
                notifyObservers(new NotificationEvent(
                        NotificationEventType.TRANSACTION_COMPLETED,
                        "Transaction executed successfully after manager approval.",
                        tx.getTransactionId(),
                        Role.CUSTOMER,
                        customerId
                ));
            }

            // (6) ✅ NEW: low balance warning after execution too
            if (from != null && ("WITHDRAW".equals(tx.getType()) || "TRANSFER".equals(tx.getType()))) {
                if (from.getBalance() <= LOW_BALANCE_THRESHOLD) {
                    notifyObservers(new NotificationEvent(
                            NotificationEventType.LOW_BALANCE_WARNING,
                            "Low balance warning: " + from.getBalance() + " on account " + from.getAccountNumber(),
                            tx.getTransactionId(),
                            Role.CUSTOMER,
                            from.ownerId
                    ));
                }
            }

        } catch (Exception e) {
            transactionDAO.markExecuted(transactionId, "FAILED: " + e.getMessage());

            // ✅ NEW: validation failed
            if (customerId != null) {
                notifyObservers(new NotificationEvent(
                        NotificationEventType.TRANSACTION_VALIDATION_FAILED,
                        "Validation failed: " + e.getMessage(),
                        tx.getTransactionId(),
                        Role.CUSTOMER,
                        customerId
                ));
            }

            // existing failed
            if (customerId != null) {
                notifyObservers(new NotificationEvent(
                        NotificationEventType.TRANSACTION_FAILED,
                        "Transaction execution failed: " + e.getMessage(),
                        tx.getTransactionId(),
                        Role.CUSTOMER,
                        customerId
                ));
            }
        }
    }
}

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