package com.bankingSystem.Account.DecoratorPattern;

public class InsufficientFunds extends RuntimeException {
    public InsufficientFunds(double currentBalance, double withdrawAmount) {
        super("Tried to withdraw amount" + withdrawAmount + " bigger than your current balance " + currentBalance);
    }
}
