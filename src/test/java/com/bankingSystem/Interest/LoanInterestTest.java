package com.bankingSystem.Interest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoanInterestTest {

    @Test
    void calculateInterest_debtBalance_shouldReturnMonthlyInterest() {
        LoanInterest l = new LoanInterest();
        // debt = 1200, annual 7% => monthly = 1200 * 0.07 / 12 = 7.0
        assertEquals(7.0, l.calculateInterest(-1200.0), 0.0001);
    }
}
