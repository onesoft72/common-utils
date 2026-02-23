package com.onesoft.common.validation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValidatorTest {

    @Test
    void testIsPositive() {
        assertTrue(Validator.isPositive(10));
        assertFalse(Validator.isPositive(-1));
    }
}