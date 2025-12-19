package com.bankingSystem.Account.DecoratorPattern;

import com.bankingSystem.Account.CompositePattern.AccountComponent;

public class OverdraftProtectionDecorator extends AccountDecorator {
    private final double withdrawLimit;

    public OverdraftProtectionDecorator(AccountComponent account, double withdrawLimit) {
        super(account);
        this.withdrawLimit = withdrawLimit;
    }

    @Override
    public void withdraw(double amount) {
        double allowedAmount = account.getTotalBalance() + withdrawLimit;
        //if(amount > account.getTotalBalance()) throw new InsufficientFunds(account.getTotalBalance(), amount);
        if(amount > allowedAmount) throw new LimitReached(withdrawLimit, amount);

        super.withdraw(amount);
    }
}
