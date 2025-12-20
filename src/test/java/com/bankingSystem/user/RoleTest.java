package com.bankingSystem.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    @Test
    void fromString_shouldAcceptEnumNameOrDisplayName() {
        assertEquals(Role.CUSTOMER, Role.fromString("CUSTOMER"));
        assertEquals(Role.CUSTOMER, Role.fromString("Customer"));
        assertEquals(Role.MANAGER, Role.fromString("manager"));
        assertEquals(Role.TELLER, Role.fromString("Teller"));
    }

    @Test
    void fromString_unknown_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> Role.fromString("Nope"));
    }
}
