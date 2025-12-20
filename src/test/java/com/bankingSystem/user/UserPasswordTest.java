package com.bankingSystem.user;

import com.bankingSystem.Database.UserDAO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserPasswordTest {

    @Test
    void checkPassword_shouldReturnTrue_forCorrectPassword() {
        Customer c = new Customer("user1", "pass123", "0999");
        assertTrue(c.checkPassword("pass123"));
        assertFalse(c.checkPassword("wrong"));
    }

    @Test
    void checkPassword_nullInput_shouldReturnFalse() {
        Customer c = new Customer("user1", "pass123", "0999");
        assertFalse(c.checkPassword(null));
    }

    @Test
    void role_fromString_shouldAcceptDisplayNameAndEnumName() {
        assertEquals(Role.MANAGER, Role.fromString("Manager"));
        assertEquals(Role.MANAGER, Role.fromString("MANAGER"));
    }
    @Test
    void role_fromString_invalid_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> Role.fromString("x"));
    }




}
