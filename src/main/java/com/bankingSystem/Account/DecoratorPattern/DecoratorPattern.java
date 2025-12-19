package com.bankingSystem.Account.DecoratorPattern;

import com.bankingSystem.Account.CheckingAccount;
import com.bankingSystem.Account.CompositePattern.AccountComponent;


public class DecoratorPattern {

    public static void main(String[] args) {

        System.out.println("===== ACCOUNT CREATION =====");

        AccountComponent account =
                new CheckingAccount("0987654321", "default-customer-001");

        account.showDetails();
        System.out.println("Initial balance: " + account.getTotalBalance());
        System.out.println();

        System.out.println("===== DEPOSIT TEST =====");

        account.deposit(1000);
        System.out.println("Deposited 1000");
        System.out.println("Balance after deposit: " + account.getTotalBalance());
        account.showDetails();
        System.out.println();

        System.out.println("===== NORMAL WITHDRAW TEST =====");

        account.withdraw(300);
        System.out.println("Withdrew 300");
        System.out.println("Balance after withdrawal: " + account.getTotalBalance());
        account.showDetails();
        System.out.println();

        System.out.println("===== APPLY OVERDRAFT DECORATOR (LIMIT = 500) =====");

        account = new OverdraftProtectionDecorator(account, 500);
        System.out.println("Decorator applied");
        System.out.println("Effective balance + overdraft = "
                + (account.getTotalBalance() + 500));
        System.out.println();

        System.out.println("===== OVERDRAFT WITHIN LIMIT =====");

        account.withdraw(1000);
        System.out.println("Withdrew 1000 using overdraft");
        System.out.println("Balance after overdraft: " + account.getTotalBalance());
        account.showDetails();
        System.out.println();

        System.out.println("===== OVERDRAFT LIMIT BREACH (SHOULD FAIL) =====");

        try {
            account.withdraw(300);
            System.out.println("ERROR: Withdrawal should NOT have succeeded");
        } catch (Exception e) {
            System.out.println("Expected exception caught:");
            System.out.println(e.getClass().getSimpleName() + " → " + e.getMessage());
        }

        System.out.println();
        System.out.println("===== FINAL ACCOUNT STATE =====");
        account.showDetails();
        System.out.println("Final balance: " + account.getTotalBalance());

        System.out.println("===== TEST COMPLETE =====");
    }
}
