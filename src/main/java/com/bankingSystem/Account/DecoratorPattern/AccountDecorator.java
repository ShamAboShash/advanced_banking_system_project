package com.bankingSystem.Account.DecoratorPattern;
 import com.bankingSystem.Account.Account;
 import com.bankingSystem.Account.CompositePattern.AccountComponent;

public  abstract class AccountDecorator implements AccountComponent {
    protected AccountComponent account;

    protected AccountDecorator(AccountComponent account) {
        // super(account.accountId, account.accountNumber);
        this.account = account;
    }
    @Override
    public void deposit(double amount) {
        account.deposit(amount);
    }
    @Override
    public void withdraw(double amount) {
        account.withdraw(amount);
    }

    @Override
    public double getTotalBalance() {
        return account.getTotalBalance();
    }

    @Override
    public void showDetails() {
        account.showDetails();
    }
}
