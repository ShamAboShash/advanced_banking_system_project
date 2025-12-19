// src/main/java/com/bankingSystem/Transaction/Transaction.java
package com.bankingSystem.Transaction;

import java.time.LocalDateTime;
import java.util.UUID;

public class Transaction {
    private String transactionId;
    private String type;
    private double amount;
    private LocalDateTime timestamp;
    private String fromAccount;
    private String toAccount;
    private String status;
    private String description;
    private String performedBy;     // NEW: User ID who performed the transaction
    private LocalDateTime performedAt; // NEW: Exact time of execution

    // Constructor for new transactions (adds performedBy and performedAt)
    public Transaction(String type, double amount, String fromAccount, String toAccount, String performedBy) {
        this.transactionId = UUID.randomUUID().toString();
        this.type = type;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.status = "PENDING";
        this.description = String.format("%s %.2f from %s to %s", type, amount,
                fromAccount != null ? fromAccount : "-", toAccount != null ? toAccount : "-");
        this.performedBy = performedBy;
        this.performedAt = LocalDateTime.now();
    }

    // Constructor for loading from DB (includes new fields)
    public Transaction(String transactionId, String type, double amount, LocalDateTime timestamp,
                       String fromAccount, String toAccount, String status, String description,
                       String performedBy, LocalDateTime performedAt) {
        this.transactionId = transactionId;
        this.type = type;
        this.amount = amount;
        this.timestamp = timestamp;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.status = status;
        this.description = description;
        this.performedBy = performedBy;
        this.performedAt = performedAt;
    }

    // Old constructors for compatibility (call new one with null performer)
    public Transaction(String type, double amount) {
        this(type, amount, null, null, null);
    }

    public Transaction(String type, double amount, String fromAccount, String toAccount) {
        this(type, amount, fromAccount, toAccount, null);
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isApproved() {
        return status != null && (status.startsWith("APPROVED") || "COMPLETED".equals(status));
    }

    public boolean isPendingManagerApproval() {
        return "PENDING_MANAGER_APPROVAL".equals(status);
    }

    // Getters
    public String getTransactionId() { return transactionId; }
    public String getType() { return type; }
    public double getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getFromAccount() { return fromAccount; }
    public String getToAccount() { return toAccount; }
    public String getStatus() { return status; }
    public String getDescription() { return description; }
    public String getPerformedBy() { return performedBy; }
    public LocalDateTime getPerformedAt() { return performedAt; }

    @Override
    public String toString() {
        String performer = performedBy != null ? performedBy : "unknown";
        String time = performedAt != null ? performedAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : timestamp.toString().substring(0, 16);
        return String.format("[%s] %s | %.2f | %s → %s | By: %s | Status: %s | %s",
                time, type, amount,
                fromAccount != null ? fromAccount : "-",
                toAccount != null ? toAccount : "-",
                performer, status, description);
    }
}

//package com.bankingSystem.Transaction;
//
//import java.time.LocalDateTime;
//import java.util.UUID;
//
//public class Transaction {
//    private String transactionId;
//    private String type; // DEPOSIT, WITHDRAW, TRANSFER_IN, TRANSFER_OUT, LOAN_PAYMENT, INTEREST
//    private double amount;
//    private LocalDateTime timestamp;
//    private String fromAccount;
//    private String toAccount;
//    private String status; // PENDING, APPROVED, REJECTED, COMPLETED
//    private String description;    // ====================== Original Constructors (unchanged) ======================
//    private String performedBy;     // ← NEW: who did it (userId)
//    private LocalDateTime performedAt;
//
//
//    public Transaction(String type, double amount) {
//        this.transactionId = UUID.randomUUID().toString();
//        this.type = type;
//        this.amount = amount;
//        this.timestamp = LocalDateTime.now();
//        this.status = "PENDING";
//        this.description = type + " of " + String.format("%.2f", amount);
//    }
//
//    public Transaction(String type, double amount, String fromAccount, String toAccount) {
//        this(type, amount);
//        this.fromAccount = fromAccount;
//        this.toAccount = toAccount;
//        this.description = String.format("%s %.2f from %s to %s", type, amount,
//                fromAccount != null ? fromAccount : "-", toAccount != null ? toAccount : "-");
//    }
//
//    // ====================== NEW Constructor for DAO loading (from DB) ======================
//    // هذا المُنشئ للاستخدام داخلي فقط عند تحميل البيانات من قاعدة البيانات
//    public Transaction(String transactionId, String type, double amount, LocalDateTime timestamp,
//                       String fromAccount, String toAccount, String status, String description) {
//        this.transactionId = transactionId;
//        this.type = type;
//        this.amount = amount;
//        this.timestamp = timestamp;
//        this.fromAccount = fromAccount;
//        this.toAccount = toAccount;
//        this.status = status;
//        this.description = description;
//    }
//
//    // ====================== Setters ======================
//    public void setStatus(String status) {
//        this.status = status;
//    }
//
//    // ====================== Helper Methods ======================
//    public boolean isApproved() {
//        return status != null && (status.startsWith("APPROVED") || "COMPLETED".equals(status));
//    }
//
//    public boolean isPendingManagerApproval() {
//        return "PENDING_MANAGER_APPROVAL".equals(status);
//    }
//
//    // ====================== Getters ======================
//    public String getTransactionId() { return transactionId; }
//    public String getType() { return type; }
//    public double getAmount() { return amount; }
//    public LocalDateTime getTimestamp() { return timestamp; }
//    public String getFromAccount() { return fromAccount; }
//    public String getToAccount() { return toAccount; }
//    public String getStatus() { return status; }
//    public String getDescription() { return description; }
//
//    // ====================== toString ======================
//    @Override
//    public String toString() {
//        return String.format("[%s] %s | %.2f | %s → %s | Status: %-25s | %s",
//                timestamp.toString().substring(0, 19).replace("T", " "),
//                type,
//                amount,
//                fromAccount != null ? fromAccount : "-",
//                toAccount != null ? toAccount : "-",
//                status,
//                description);
//    }
//}