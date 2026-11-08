package com.pagoflex.core.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RutUtilsTest {
    @Test
    void validatesModuloEleven() {
        assertTrue(RutUtils.isValid("99.999.999-9"));
        assertTrue(RutUtils.isValid("  999999999  "));
        assertTrue(RutUtils.isValid("99.999.998-0"));
        assertTrue(RutUtils.isValid("6-k"));
        assertFalse(RutUtils.isValid("99.999.999-8"));
        assertFalse(RutUtils.isValid("99.999.999-X"));
        assertFalse(RutUtils.isValid(""));
        assertFalse(RutUtils.isValid("   "));
        assertFalse(RutUtils.isValid(null));
    }

    @Test
    void normalizesToBodyAndVerificationDigitWithoutDots() {
        assertEquals("99999999-9", RutUtils.normalize("  99.999.999-9  "));
        assertEquals("99999998-0", RutUtils.normalize("999999980"));
        assertEquals("6-K", RutUtils.normalize("6-k"));
        assertEquals("99999999-9", RutUtils.normalize("99999999-9"));
        assertThrows(IllegalArgumentException.class, () -> RutUtils.normalize("99.999.999-8"));
        assertThrows(IllegalArgumentException.class, () -> RutUtils.normalize(null));
    }
}
