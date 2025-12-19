// src/main/java/com/bankingSystem/Database/TransactionDAO.java
package com.bankingSystem.Database;

import com.bankingSystem.Transaction.Transaction;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {
    private final Connection conn = DatabaseConnection.getInstance().getConnection();

    /**
     * Save a transaction with performer and exact execution time
     */
    public void saveTransaction(Transaction tx) {
        String sql = """
            INSERT INTO Transactions 
            (transactionId, type, amount, timestamp, fromAccount, toAccount, 
             status, description, performedBy, performedAt) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tx.getTransactionId());
            pstmt.setString(2, tx.getType());
            pstmt.setDouble(3, tx.getAmount());
            pstmt.setObject(4, tx.getTimestamp());
            pstmt.setString(5, tx.getFromAccount());
            pstmt.setString(6, tx.getToAccount());
            pstmt.setString(7, tx.getStatus());
            pstmt.setString(8, tx.getDescription());
            pstmt.setString(9, tx.getPerformedBy());     // ← NEW: who did it
            pstmt.setObject(10, tx.getPerformedAt());    // ← NEW: when exactly
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving transaction: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<Transaction> loadTransactions(String accountNumber) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = """
            SELECT * FROM Transactions 
            WHERE fromAccount = ? OR toAccount = ?
            ORDER BY performedAt DESC
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountNumber);
            pstmt.setString(2, accountNumber);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(buildTransactionFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading transactions for account: " + accountNumber);
            e.printStackTrace();
        }

        return transactions;
    }

    public List<Transaction> loadAllTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM Transactions ORDER BY performedAt DESC";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                transactions.add(buildTransactionFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error loading all transactions");
            e.printStackTrace();
        }

        return transactions;
    }

    public List<Transaction> loadPendingTransactions() {
        List<Transaction> pending = new ArrayList<>();
        String sql = "SELECT * FROM Transactions WHERE status = 'PENDING_MANAGER_APPROVAL'";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                pending.add(buildTransactionFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pending;
    }

    public void updateTransactionStatus(String transactionId, String newStatus) {
        String sql = "UPDATE Transactions SET status = ? WHERE transactionId = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setString(2, transactionId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating transaction status: " + e.getMessage());
        }
    }

    public Transaction loadTransactionById(String transactionId) {
        String sql = "SELECT * FROM Transactions WHERE transactionId = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return buildTransactionFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading transaction by ID: " + transactionId);
            e.printStackTrace();
        }
        return null;
    }

    // ============ NEW: Manager Reports ============

    /**
     * Get all transactions for a specific account on today's date
     */
    public List<Transaction> getDailyTransactionsForAccount(String accountNumber) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = """
            SELECT * FROM Transactions 
            WHERE (fromAccount = ? OR toAccount = ?) 
            AND DATE(performedAt) = CURRENT_DATE
            ORDER BY performedAt DESC
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountNumber);
            pstmt.setString(2, accountNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(buildTransactionFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }

    /**
     * Get all transactions for a specific account in the last 7 days
     */
    public List<Transaction> getWeeklyTransactionsForAccount(String accountNumber) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = """
            SELECT * FROM Transactions 
            WHERE (fromAccount = ? OR toAccount = ?) 
            AND performedAt >= DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY)
            ORDER BY performedAt DESC
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountNumber);
            pstmt.setString(2, accountNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(buildTransactionFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }

    /**
     * Get all accounts that have transactions today (for manager report summary)
     */
//    public List<String> getAccountsWithTransactionsToday() {
//        List<String> accounts = new ArrayList<>();
//        String sql = """
//            SELECT DISTINCT fromAccount AS acc FROM Transactions WHERE DATE(performedAt) = CURRENT_DATE
//            UNION
//            SELECT DISTINCT toAccount AS acc FROM Transactions WHERE DATE(performedAt) = CURRENT_DATE
//            """;
//
//        try (Statement stmt = conn.createStatement();
//             ResultSet rs = stmt.executeQuery(sql)) {
//            while (rs.next()) {
//                String acc = rs.getString("acc");
//                if (acc != null) accounts.add(acc);
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return accounts;
//    }

    // Helper to build Transaction with new fields
    private Transaction buildTransactionFromResultSet(ResultSet rs) throws SQLException {
        return new Transaction(
                rs.getString("transactionId"),
                rs.getString("type"),
                rs.getDouble("amount"),
                rs.getObject("timestamp", LocalDateTime.class),
                rs.getString("fromAccount"),
                rs.getString("toAccount"),
                rs.getString("status"),
                rs.getString("description"),
                rs.getString("performedBy"),
                rs.getObject("performedAt", LocalDateTime.class)
        );
    }

    // Add these to TransactionDAO.java
    public List<String> getAccountsWithTransactionsToday() {
        // Simplified version using loadAllTransactions + filter
        List<String> accounts = new ArrayList<>();
        for (Transaction tx : loadAllTransactions()) {
            LocalDate txDate = tx.getPerformedAt().toLocalDate();
            if (txDate.equals(LocalDate.now())) {
                if (tx.getFromAccount() != null) accounts.add(tx.getFromAccount());
                if (tx.getToAccount() != null) accounts.add(tx.getToAccount());
            }
        }
        return accounts.stream().distinct().toList();
    }

    public List<String> getAccountsWithTransactionsInWeek() {
        List<String> accounts = new ArrayList<>();
        LocalDate weekAgo = LocalDate.now().minusDays(7);
        for (Transaction tx : loadAllTransactions()) {
            LocalDate txDate = tx.getPerformedAt().toLocalDate();
            if (!txDate.isBefore(weekAgo)) {
                if (tx.getFromAccount() != null) accounts.add(tx.getFromAccount());
                if (tx.getToAccount() != null) accounts.add(tx.getToAccount());
            }
        }
        return accounts.stream().distinct().toList();
    }
}