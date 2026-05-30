package run.ikaros.api.infra.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AssertUtilsTest {

    @Test
    void isTrue_WithTrueCondition_ShouldNotThrowException() {
        assertDoesNotThrow(() -> AssertUtils.isTrue(true, "Should not throw"));
    }

    @Test
    void isTrue_WithFalseCondition_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> AssertUtils.isTrue(false, "Test message")
        );
        assertEquals("'Test message' must be true", exception.getMessage());
    }

    @Test
    void isTrue_WithTrueConditionAndNullMessage_ShouldNotThrowException() {
        assertDoesNotThrow(() -> AssertUtils.isTrue(true, null));
    }

    @Test
    void isTrue_WithFalseConditionAndNullMessage_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> AssertUtils.isTrue(false, null)
        );
        assertEquals("'null' must be true", exception.getMessage());
    }

    @Test
    void isTrue_WithTrueConditionAndEmptyMessage_ShouldNotThrowException() {
        assertDoesNotThrow(() -> AssertUtils.isTrue(true, ""));
    }

    @Test
    void isTrue_WithFalseConditionAndEmptyMessage_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> AssertUtils.isTrue(false, "")
        );
        assertEquals("'' must be true", exception.getMessage());
    }
}
