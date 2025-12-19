// src/main/java/com/bankingSystem/Database/DatabaseConnection.java
package com.bankingSystem.Database;

import org.mindrot.jbcrypt.BCrypt;  // ← NEW IMPORT
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() {
        try {
            connection = DriverManager.getConnection(
                    "jdbc:h2:./data/bank;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE",
                    "sa",
                    ""
            );
            initializeDatabase();
        } catch (SQLException e) {
            throw new RuntimeException("Error connecting to database", e);
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    private void initializeDatabase() throws SQLException {
        String createUsers = "CREATE TABLE IF NOT EXISTS Users (" +
                "id VARCHAR(255) PRIMARY KEY, " +
                "username VARCHAR(255), " +
                "password VARCHAR(255), " +           // Stores BCrypt hash
                "phoneNumber VARCHAR(20) UNIQUE, " +
                "role VARCHAR(50)" +
                ")";

        String createAccounts = "CREATE TABLE IF NOT EXISTS Accounts (" +
                "accountId VARCHAR(255) PRIMARY KEY, " +
                "accountNumber VARCHAR(255), " +
                "balance DOUBLE, " +
                "creationDate TIMESTAMP, " +
                "ownerId VARCHAR(255), " +
                "state VARCHAR(50), " +
                "type VARCHAR(50), " +
                "loanAmount DOUBLE NULL" +
                ")";

        String createTransactions = "CREATE TABLE IF NOT EXISTS Transactions (" +
                "transactionId VARCHAR(255) PRIMARY KEY, " +
                "type VARCHAR(50), " +
                "amount DOUBLE, " +
                "timestamp TIMESTAMP, " +
                "fromAccount VARCHAR(255), " +
                "toAccount VARCHAR(255), " +
                "status VARCHAR(50), " +
                "description VARCHAR(255), " +
                "performedBy VARCHAR(255), " +
                "performedAt TIMESTAMP" +
                ")";

        connection.createStatement().execute(createUsers);
        connection.createStatement().execute(createAccounts);
        connection.createStatement().execute(createTransactions);

        // === INSERT DEFAULT USERS WITH HASHED PASSWORDS ===
        String hashedCustomer = BCrypt.hashpw("pass123", BCrypt.gensalt());
        String hashedTeller = BCrypt.hashpw("tellerpass", BCrypt.gensalt());
        String hashedManager = BCrypt.hashpw("mgrpass", BCrypt.gensalt());
        String hashedAdmin = BCrypt.hashpw("adminpass", BCrypt.gensalt());
        String mergeDefaultUsers = """
            MERGE INTO Users KEY(id) VALUES
            ('default-customer-001', 'customer1', '%s', '0987654321', 'Customer'),
            ('default-teller-001',   'teller1',    '%s', '0987654322', 'Teller'),
            ('default-manager-001',  'manager1',   '%s', '0987654333', 'Manager'),
            ('default-admin-001',    'admin',      '%s', '0987655555', 'Admin')
            """.formatted(hashedCustomer, hashedTeller, hashedManager, hashedAdmin);

        try {
            connection.createStatement().execute(mergeDefaultUsers);
            System.out.println("Default users created with secure hashed passwords.");
        } catch (SQLException e) {
            // Users already exist — safe to ignore
            System.out.println("Default users already exist.");
        }
    }
}