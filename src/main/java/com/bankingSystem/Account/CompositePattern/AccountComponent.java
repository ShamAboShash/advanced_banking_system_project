// src/main/java/com/bankingSystem/Account/CompositePattern/AccountComponent.java
package com.bankingSystem.Account.CompositePattern;

public interface AccountComponent {

    void showDetails();
    double getTotalBalance();

    // For Composite only
    default void add(AccountComponent component) {
        throw new UnsupportedOperationException("Cannot add sub-account to this type");
    }

    default void remove(AccountComponent component) {
        throw new UnsupportedOperationException("Cannot remove sub-account from this type");
    }
    public default void deposit(double amount) {
        throw new UnsupportedOperationException("Deposit is not supported by this component");
    }

    public default void withdraw(double amount) {
        throw new UnsupportedOperationException("Withdrawal is not supported by this component");
    }
}