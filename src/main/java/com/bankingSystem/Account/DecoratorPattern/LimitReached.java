package com.bankingSystem.Account.DecoratorPattern;

public class LimitReached extends RuntimeException {
    public LimitReached(double withdrawLimit, double withdrawAmount) {
        super("you have a withdrawal limit on your account. Tried to withdraw " + withdrawAmount + "and the limit is " + withdrawLimit);
    }
}
