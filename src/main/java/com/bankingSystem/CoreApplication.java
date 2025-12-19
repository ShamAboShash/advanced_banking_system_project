package com.bankingSystem;

import com.bankingSystem.Account.*;
import com.bankingSystem.Account.CompositePattern.AccountComponent;
import com.bankingSystem.Database.*;
import com.bankingSystem.user.*;
import com.bankingSystem.Proxy.BankingService;
import com.bankingSystem.Proxy.RoleBasedAccessProxy;
import com.bankingSystem.Transaction.Transaction;

// Imports needed for the Decorator Demo
import com.bankingSystem.Account.DecoratorPattern.OverdraftProtectionDecorator;
import com.bankingSystem.Account.DecoratorPattern.LimitReached;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CoreApplication {

    private static final Scanner scanner = new Scanner(System.in);
    private static final UserDAO userDAO = new UserDAO();
    private static final AccountDAO accountDAO = new AccountDAO();
    private static final TransactionDAO transactionDAO = new TransactionDAO();

    // Colors for pretty console
    private static class Colors {
        public static final String RESET = "\u001B[0m";
        public static final String CYAN = "\u001B[36m";
        public static final String GREEN = "\u001B[32m";
        public static final String YELLOW = "\u001B[33m";
        public static final String RED = "\u001B[31m";
        public static final String BOLD = "\u001B[1m";
        public static final String PURPLE = "\u001B[35m";
    }

    private static void printHeader(String text) {
        String line = "═".repeat(text.length() + 10);
        System.out.println(Colors.CYAN + "╔" + line + "╗" + Colors.RESET);
        System.out.println(Colors.CYAN + "║" + " ".repeat(4) + Colors.BOLD + text + Colors.RESET + " ".repeat(4) + "║");
        System.out.println(Colors.CYAN + "╚" + line + "╝" + Colors.RESET + "\n");
    }

    public static void main(String[] args) {
        printHeader("ADVANCED BANKING SYSTEM");

        while (true) {
            System.out.println("1. Customer");
            System.out.println("2. Teller");
            System.out.println("3. Manager");
            System.out.println("4. Exit");
            // --- CHANGE #1: Updated menu item to '5' ---
            System.out.println(Colors.PURPLE + "5. Run Decorator Pattern Demo" + Colors.RESET);
            System.out.print("Choose option (1-5): "); // Updated prompt
            String roleChoice = scanner.nextLine().trim();

            if (roleChoice.equals("4")) {
                System.out.println(Colors.GREEN + "Thank you for using the system. Goodbye!" + Colors.RESET);
                break;
            }

            // --- CHANGE #2: Updated trigger logic to check for '5' ---
            if (roleChoice.equals("5")) {
                runDecoratorPatternDemo();
                continue; // Go back to the main menu after the demo
            }

            Role selectedRole = switch (roleChoice) {
                case "1" -> Role.CUSTOMER;
                case "2" -> Role.TELLER;
                case "3" -> Role.MANAGER;
                default -> null;
            };

            if (selectedRole == null) {
                System.out.println(Colors.RED + "Invalid choice. Try again.\n" + Colors.RESET);
                continue;
            }

            System.out.print("Enter phone number: ");
            String phone = scanner.nextLine().trim();
            System.out.print("Enter password: ");
            String password = scanner.nextLine();

            User user = userDAO.loadUserByPhoneAndPassword(phone, password);
            if (user == null || user.getRole() != selectedRole) {
                System.out.println(Colors.RED + "Login failed: Invalid credentials or wrong role.\n" + Colors.RESET);
                continue;
            }

            System.out.println(Colors.GREEN + "Login successful! Welcome, " + user.getUsername() + " (" + user.getRole().getDisplayName() + ")" + Colors.RESET + "\n");

            BankingService service = new RoleBasedAccessProxy(user);
            roleMenuLoop(service, user);
        }
    }

    private static void roleMenuLoop(BankingService service, User currentUser) {
        // ... (This part of the code remains unchanged)
        while (true) {
            printHeader(currentUser.getRole().getDisplayName().toUpperCase() + " MENU");

            switch (currentUser.getRole()) {
                case CUSTOMER -> customerMenu();
                case TELLER -> tellerMenu();
                case MANAGER -> managerMenu();
            }

            System.out.print("Enter choice (0 to logout): ");
            String choice = scanner.nextLine().trim();

            if (choice.equals("0")) {
                System.out.println(Colors.YELLOW + "Logged out successfully.\n" + Colors.RESET);
                break;
            }

            try {
                switch (currentUser.getRole()) {
                    case CUSTOMER -> handleCustomerAction(service, currentUser, choice);
                    case TELLER -> handleTellerAction(service, choice);
                    case MANAGER -> handleManagerAction(service, choice);
                }
            } catch (SecurityException e) {
                System.out.println(Colors.RED + "Access Denied: " + e.getMessage() + Colors.RESET + "\n");
            } catch (Exception e) {
                System.out.println(Colors.RED + "Error: " + e.getMessage() + Colors.RESET + "\n");
            }
        }
    }

    // =================================================================
    // The rest of the file (menus, actions, and the demo method) remains unchanged.
    // ...
    // =================================================================

    // ==================== MENUS ====================
    private static void customerMenu() {
        System.out.println("1. View transaction history");
        System.out.println("2. Deposit money");
        System.out.println("3. Withdraw money");
        System.out.println("4. Transfer money");
        System.out.println("5. View my accounts");
    }

    private static void tellerMenu() {
        System.out.println("1. Create new customer and account");
        System.out.println("2. Add account to existing customer");
        System.out.println("3. Find account by number");
        System.out.println("4. Change account state");
        System.out.println("5. Perform transaction on any account");
    }

    private static void managerMenu() {
        System.out.println("1. View all customers");
        System.out.println("2. View all tellers");
        System.out.println("3. Add new teller");
        System.out.println("4. Delete teller");
        System.out.println("5. Daily transaction report per account");
        System.out.println("6. Weekly transaction report per account");
        System.out.println("7. All transactions report");
    }

    // ==================== CUSTOMER ACTIONS ====================
    private static void handleCustomerAction(BankingService service, User user, String choice) {
        // ... (This method remains unchanged)
        List<Account> myAccounts = accountDAO.getAccountsByOwner(user.getUserId());

        if (myAccounts.isEmpty()) {
            System.out.println(Colors.YELLOW + "You have no accounts yet. Contact a teller.\n" + Colors.RESET);
            return;
        }

        switch (choice) {
            case "1" -> viewCustomerHistory(service, user, myAccounts);
            case "2", "3", "4" -> performCustomerTransaction(service, user, myAccounts, choice);
            case "5" -> viewCustomerAccounts(myAccounts);
            default -> System.out.println(Colors.RED + "Invalid choice.\n" + Colors.RESET);
        }
    }

    private static void viewCustomerHistory(BankingService service, User user, List<Account> accounts) {
        // ... (This method remains unchanged)
        System.out.println("Choose account:");
        for (int i = 0; i < accounts.size(); i++) {
            System.out.println((i + 1) + ". " + accounts.get(i).getAccountNumber());
        }
        int idx = Integer.parseInt(scanner.nextLine()) - 1;
        if (idx < 0 || idx >= accounts.size()) {
            System.out.println(Colors.RED + "Invalid selection.\n" + Colors.RESET);
            return;
        }
        Account acc = accounts.get(idx);
        List<Transaction> history = service.getTransactionHistory(user.getUserId(), acc.getAccountNumber());
        printHeader("TRANSACTION HISTORY - " + acc.getAccountNumber());
        if (history.isEmpty()) {
            System.out.println("No transactions yet.\n");
        } else {
            history.forEach(System.out::println);
        }
    }

    private static void performCustomerTransaction(BankingService service, User user, List<Account> accounts, String action) {
        // ... (This method remains unchanged)
        System.out.println("Choose account:");
        for (int i = 0; i < accounts.size(); i++) {
            System.out.println((i + 1) + ". " + accounts.get(i));
        }
        int idx = Integer.parseInt(scanner.nextLine()) - 1;
        if (idx < 0 || idx >= accounts.size()) {
            System.out.println(Colors.RED + "Invalid account.\n" + Colors.RESET);
            return;
        }
        Account primary = accounts.get(idx);

        String type = switch (action) {
            case "2" -> "DEPOSIT";
            case "3" -> "WITHDRAW";
            case "4" -> "TRANSFER";
            default -> null;
        };

        Account to = null;
        if ("TRANSFER".equals(type)) {
            System.out.println("Choose destination account:");
            for (int i = 0; i < accounts.size(); i++) {
                if (i != idx) System.out.println((i + 1) + ". " + accounts.get(i));
            }
            int toIdx = Integer.parseInt(scanner.nextLine()) - 1;
            if (toIdx < 0 || toIdx >= accounts.size() || toIdx == idx) {
                System.out.println(Colors.RED + "Invalid destination.\n" + Colors.RESET);
                return;
            }
            to = accounts.get(toIdx);
        }

        System.out.print("Amount: ");
        double amount = Double.parseDouble(scanner.nextLine());

        service.performTransaction(user.getUserId(),
                type.equals("WITHDRAW") || type.equals("TRANSFER") ? primary : null,
                type.equals("DEPOSIT") || type.equals("TRANSFER") ? (to != null ? to : primary) : null,
                amount, type);
        System.out.println();
    }

    private static void viewCustomerAccounts(List<Account> accounts) {
        // ... (This method remains unchanged)
        printHeader("YOUR ACCOUNTS");
        accounts.forEach(acc -> {
            acc.showDetails();
            System.out.println();
        });
    }

    // ==================== TELLER ACTIONS ====================
    private static void handleTellerAction(BankingService service, String choice) {
        // ... (This method remains unchanged)
        switch (choice) {
            case "1" -> tellerCreateCustomerAndAccount(service);
            case "2" -> tellerAddAccountToExisting(service);
            case "3" -> tellerFindAccount(service);
            case "4" -> tellerChangeState(service);
            case "5" -> tellerPerformTransaction(service);
            default -> System.out.println(Colors.RED + "Invalid choice.\n" + Colors.RESET);
        }
    }

    private static void tellerCreateCustomerAndAccount(BankingService service) {
        // ... (This method remains unchanged)
        System.out.print("New customer username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Phone number: ");
        String phone = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        Customer newCustomer = new Customer(username, password, phone);
        newCustomer.persist();
        System.out.println(Colors.GREEN + "New customer created: " + newCustomer.getUsername() + Colors.RESET);

        System.out.print("Account type (savings/checking/loan/investment): ");
        String type = scanner.nextLine().trim();
        Account acc = service.createAccount(type, newCustomer.getUserId());
        System.out.println(Colors.GREEN + "Account created: " + acc.getAccountNumber() + Colors.RESET + "\n");
    }

    private static void tellerAddAccountToExisting(BankingService service) {
        // ... (This method remains unchanged)
        System.out.print("Customer phone number: ");
        String phone = scanner.nextLine().trim();
        User owner = userDAO.loadUserByPhone(phone);
        if (owner == null) {
            System.out.println(Colors.RED + "Customer not found.\n" + Colors.RESET);
            return;
        }

        System.out.print("Account type (savings/checking/loan/investment): ");
        String type = scanner.nextLine().trim();
        Account acc = service.createAccount(type, owner.getUserId());
        System.out.println(Colors.GREEN + "Account added to " + owner.getUsername() + ": " + acc.getAccountNumber() + Colors.RESET + "\n");
    }

    private static void tellerFindAccount(BankingService service) {
        // ... (This method remains unchanged)
        System.out.print("Account number: ");
        String num = scanner.nextLine().trim();
        Account acc = service.findAccountByNumber(num);
        if (acc != null) {
            printHeader("ACCOUNT FOUND");
            acc.showDetails();
        } else {
            System.out.println(Colors.RED + "Account not found.\n" + Colors.RESET);
        }
    }

    private static void tellerChangeState(BankingService service) {
        // ... (This method remains unchanged)
        System.out.print("Account number: ");
        String num = scanner.nextLine().trim();
        System.out.print("New state (active/frozen/suspended/closed): ");
        String state = scanner.nextLine().trim();
        service.changeAccountState(num, state);
        System.out.println(Colors.GREEN + "State updated.\n" + Colors.RESET);
    }

    private static void tellerPerformTransaction(BankingService service) {
        // ... (This method remains unchanged)
        System.out.println("Transaction type:");
        System.out.println("1. Deposit");
        System.out.println("2. Withdraw");
        System.out.println("3. Transfer");
        System.out.print("Choice: ");
        String typeChoice = scanner.nextLine().trim();

        String type = switch (typeChoice) {
            case "1" -> "DEPOSIT";
            case "2" -> "WITHDRAW";
            case "3" -> "TRANSFER";
            default -> null;
        };
        if (type == null) {
            System.out.println(Colors.RED + "Invalid type.\n" + Colors.RESET);
            return;
        }

        Account from = null;
        Account to = null;

        if (!"DEPOSIT".equals(type)) {
            System.out.print("From account number: ");
            String fromNum = scanner.nextLine().trim();
            from = accountDAO.loadAccount(fromNum);
            if (from == null) {
                System.out.println(Colors.RED + "From account not found.\n" + Colors.RESET);
                return;
            }
        }

        if (!"WITHDRAW".equals(type)) {
            System.out.print("To account number: ");
            String toNum = scanner.nextLine().trim();
            to = accountDAO.loadAccount(toNum);
            if (to == null) {
                System.out.println(Colors.RED + "To account not found.\n" + Colors.RESET);
                return;
            }
        }

        System.out.print("Amount: ");
        double amount = Double.parseDouble(scanner.nextLine());

        service.performTransaction("teller", from, to, amount, type);
        System.out.println(Colors.GREEN + "Transaction completed.\n" + Colors.RESET);
    }

    // ==================== MANAGER ACTIONS ====================
    private static void handleManagerAction(BankingService service, String choice) {
        // ... (This method remains unchanged)
        switch (choice) {
            case "1" -> viewAllCustomers(service);
            case "2" -> viewAllTellers(service);
            case "3" -> addNewTeller(service);
            case "4" -> deleteTeller(service);
            case "5" -> dailyReportPerAccount();
            case "6" -> weeklyReportPerAccount();
            case "7" -> viewAllTransactions(service);
            default -> System.out.println(Colors.RED + "Invalid choice.\n" + Colors.RESET);
        }
    }

    private static void viewAllCustomers(BankingService service) {
        // ... (This method remains unchanged)
        List<User> customers = service.getAllCustomers();
        printHeader("ALL CUSTOMERS (" + customers.size() + ")");
        customers.forEach(System.out::println);
    }

    private static void viewAllTellers(BankingService service) {
        // ... (This method remains unchanged)
        List<User> tellers = service.getAllTellers();
        printHeader("ALL TELLERS (" + tellers.size() + ")");
        tellers.forEach(System.out::println);
    }

    private static void addNewTeller(BankingService service) {
        // ... (This method remains unchanged)
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Phone number: ");
        String phone = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        service.addTeller(username, password, phone);
        System.out.println(Colors.GREEN + "Teller added successfully.\n" + Colors.RESET);
    }

    private static void deleteTeller(BankingService service) {
        // ... (This method remains unchanged)
        System.out.print("Teller user ID to delete: ");
        String id = scanner.nextLine().trim();
        service.deleteTeller(id);
        System.out.println(Colors.YELLOW + "Teller deleted if existed.\n" + Colors.RESET);
    }

    private static void dailyReportPerAccount() {
        // ... (This method remains unchanged)
        List<String> accounts = transactionDAO.getAccountsWithTransactionsToday();
        printHeader("DAILY TRANSACTION REPORT - " + LocalDate.now());
        if (accounts.isEmpty()) {
            System.out.println("No transactions today.\n");
            return;
        }
        for (String accNum : accounts) {
            Account acc = accountDAO.loadAccount(accNum);
            System.out.println(Colors.BOLD + accNum + " | " + (acc != null ? acc.getType() : "Unknown") + Colors.RESET);
            List<Transaction> txs = transactionDAO.getDailyTransactionsForAccount(accNum);
            txs.forEach(tx -> System.out.println("  → " + tx));
            System.out.println();
        }
    }

    private static void weeklyReportPerAccount() {
        // ... (This method remains unchanged)
        List<String> accounts = transactionDAO.getAccountsWithTransactionsInWeek();
        printHeader("WEEKLY TRANSACTION REPORT");
        if (accounts.isEmpty()) {
            System.out.println("No transactions this week.\n");
            return;
        }
        for (String accNum : accounts) {
            Account acc = accountDAO.loadAccount(accNum);
            System.out.println(Colors.BOLD + accNum + " | " + (acc != null ? acc.getType() : "Unknown") + Colors.RESET);
            List<Transaction> txs = transactionDAO.getWeeklyTransactionsForAccount(accNum);
            txs.forEach(tx -> System.out.println("  → " + tx));
            System.out.println();
        }
    }

    private static void viewAllTransactions(BankingService service) {
        // ... (This method remains unchanged)
        List<Transaction> allTx = service.getAllTransactionsReport();
        printHeader("ALL TRANSACTIONS (" + allTx.size() + ")");
        allTx.forEach(System.out::println);
    }


    // =================================================================
    // ==================== NEW DECORATOR PATTERN DEMO ===================
    // =================================================================
    /**
     * This method is a self-contained demonstration for the Decorator Pattern.
     * It shows how an account can be "decorated" with new functionality (Overdraft)
     * at runtime without changing the original Account class.
     */
    private static void runDecoratorPatternDemo() {
        printHeader("DECORATOR PATTERN DEMO");

        System.out.println(Colors.YELLOW + "This is a self-contained demo and does not affect the main database." + Colors.RESET);

        System.out.println("\n===== 1. CREATING A BASE ACCOUNT =====");
        // We use a real CheckingAccount from the project for this demo.
        AccountComponent account = new CheckingAccount("DEMO-123", "demo-user");
        System.out.println("Base CheckingAccount created.");
        account.deposit(1000);
        System.out.println("Initial deposit of 1000. Current Balance: " + account.getTotalBalance());
        account.withdraw(300);
        System.out.println("Normal withdrawal of 300. Current Balance: " + account.getTotalBalance());
        System.out.println();

        System.out.println("===== 2. APPLYING OVERDRAFT DECORATOR (LIMIT = 500) =====");
        account = new OverdraftProtectionDecorator(account, 500);
        System.out.println("Decorator applied. The account object is now wrapped.");
        System.out.println();

        System.out.println("===== 3. TESTING OVERDRAFT (WITHIN LIMIT) =====");
        System.out.println("Attempting to withdraw 1000 (from 700 balance)...");
        try {
            account.withdraw(1000);
            System.out.println(Colors.GREEN + "SUCCESS: Withdrawal was allowed by the decorator." + Colors.RESET);
            System.out.println("New Balance: " + account.getTotalBalance()); // Should be -300
        } catch (Exception e) {
            System.out.println(Colors.RED + "FAILURE: Withdrawal should have succeeded but failed: " + e.getMessage() + Colors.RESET);
        }
        System.out.println();

        System.out.println("===== 4. TESTING OVERDRAFT LIMIT BREACH (SHOULD FAIL) =====");
        System.out.println("Attempting to withdraw 300 more (from -300 balance)...");
        try {
            account.withdraw(300); // Should fail as it would take balance to -600, exceeding the -500 limit.
            System.out.println(Colors.RED + "FAILURE: Withdrawal should have been blocked by the decorator." + Colors.RESET);
        } catch (LimitReached e) {
            System.out.println(Colors.GREEN + "SUCCESS: Decorator correctly blocked the transaction as expected." + Colors.RESET);
            System.out.println("  -> Caught Expected Exception: " + e.getClass().getSimpleName());
        } catch (Exception e) {
            System.out.println(Colors.RED + "FAILURE: An unexpected exception was caught: " + e.getClass().getSimpleName() + Colors.RESET);
        }
        System.out.println();

        System.out.println("===== DEMO COMPLETE =====");
        System.out.println("Press Enter to return to the main menu...");
        scanner.nextLine();
    }
}
