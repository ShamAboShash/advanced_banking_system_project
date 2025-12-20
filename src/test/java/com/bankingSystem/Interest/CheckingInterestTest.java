package com.bankingSystem.Interest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CheckingInterestTest {

    @Test
    void calculateInterest_alwaysZero() {
        CheckingInterest c = new CheckingInterest();
        assertEquals(0.0, c.calculateInterest(1000.0), 0.0001);
        assertEquals(0.0, c.calculateInterest(-500.0), 0.0001);
    }
}
