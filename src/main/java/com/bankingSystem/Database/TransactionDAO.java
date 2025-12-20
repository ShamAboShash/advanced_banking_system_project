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
     * Save a transaction with performer and exact execution time.
     * NOTE: This is INSERT only. If you call it twice with same txId you will get PK violation.
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
            pstmt.setString(9, tx.getPerformedBy());           // who requested/performed
            pstmt.setObject(10, tx.getPerformedAt());          // may be null (pending)
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving transaction: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<Transaction> loadTransactions(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return new ArrayList<>();
        }

        List<Transaction> transactions = new ArrayList<>();
        String sql = """
            SELECT * FROM Transactions
            WHERE fromAccount = ? OR toAccount = ?
            ORDER BY COALESCE(performedAt, timestamp) DESC
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
        String sql = """
            SELECT * FROM Transactions
            ORDER BY COALESCE(performedAt, timestamp) DESC
            """;

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
        String sql = """
            SELECT * FROM Transactions
            WHERE status = 'PENDING_MANAGER_APPROVAL'
            ORDER BY COALESCE(performedAt, timestamp) DESC
            """;

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

    // ============ Manager Reports ============

    public List<Transaction> getDailyTransactionsForAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) return new ArrayList<>();

        List<Transaction> transactions = new ArrayList<>();

        String sql = """
        SELECT * FROM Transactions
        WHERE (fromAccount = ? OR toAccount = ?)
          AND CAST(COALESCE(performedAt, timestamp) AS DATE) = CURRENT_DATE
        ORDER BY COALESCE(performedAt, timestamp) DESC
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

    public List<Transaction> getWeeklyTransactionsForAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) return new ArrayList<>();

        List<Transaction> transactions = new ArrayList<>();

        // ✅ H2 syntax (not MySQL):
        // last 7 days including today:
        // DATEADD('DAY', -7, CURRENT_DATE)
        String sql = """
            SELECT * FROM Transactions
            WHERE (fromAccount = ? OR toAccount = ?)
              AND COALESCE(performedAt, timestamp) >= DATEADD('DAY', -7, CURRENT_DATE)
            ORDER BY COALESCE(performedAt, timestamp) DESC
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

    public List<String> getAccountsWithTransactionsToday() {
        List<String> accounts = new ArrayList<>();

        String sql = """
    SELECT DISTINCT acc FROM (
        SELECT fromAccount AS acc
        FROM Transactions
        WHERE fromAccount IS NOT NULL
          AND CAST(COALESCE(performedAt, timestamp) AS DATE) = CURRENT_DATE

        UNION

        SELECT toAccount AS acc
        FROM Transactions
        WHERE toAccount IS NOT NULL
          AND CAST(COALESCE(performedAt, timestamp) AS DATE) = CURRENT_DATE
    )
    """;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) accounts.add(rs.getString("acc"));

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return accounts;
    }

    public List<String> getAccountsWithTransactionsInWeek() {
        List<String> accounts = new ArrayList<>();
        LocalDate weekAgo = LocalDate.now().minusDays(7);

        for (Transaction tx : loadAllTransactions()) {
            LocalDateTime t = (tx.getPerformedAt() != null) ? tx.getPerformedAt() : tx.getTimestamp();
            if (t == null) continue;

            LocalDate txDate = t.toLocalDate();
            if (!txDate.isBefore(weekAgo)) {
                if (tx.getFromAccount() != null) accounts.add(tx.getFromAccount());
                if (tx.getToAccount() != null) accounts.add(tx.getToAccount());
            }
        }
        return accounts.stream().distinct().toList();
    }

    /**
     * Mark a transaction as executed (sets performedAt).
     * Useful when manager approves then we execute later.
     */
    public void markExecuted(String transactionId, String newStatus) {
        String sql = """
            UPDATE Transactions
            SET status = ?, performedAt = CURRENT_TIMESTAMP
            WHERE transactionId = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setString(2, transactionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating execution info", e);
        }
    }
}
