package com.pagoflex.core.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EmailUtilsTest {
    @Test
    void normalizesWhitespaceAndCase() {
        assertEquals("usuario@example.com", EmailUtils.normalize("  USUARIO@EXAMPLE.COM  "));
    }

    @Test
    void preservesNullAndEmpty() {
        assertNull(EmailUtils.normalize(null));
        assertEquals("", EmailUtils.normalize("   "));
    }
}
