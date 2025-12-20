package com.bankingSystem.Interest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SavingInterestTest {

    @Test
    void calculateInterest_positiveBalance_shouldReturnFourPercent() {
        SavingInterest s = new SavingInterest();
        assertEquals(40.0, s.calculateInterest(1000.0), 0.0001);
    }

    @Test
    void calculateInterest_negativeBalance_shouldReturnZero() {
        SavingInterest s = new SavingInterest();
        assertEquals(0.0, s.calculateInterest(-1000.0), 0.0001);
    }
}
