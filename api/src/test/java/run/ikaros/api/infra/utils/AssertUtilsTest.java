package run.ikaros.api.infra.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals("Test message", exception.getMessage());
    }

    @Test
    void isTrue_WithTrueConditionAndNullMessage_ShouldNotThrowException() {
        assertDoesNotThrow(() -> AssertUtils.isTrue(true, null));
    }

    @Test
    void isTrue_WithFalseConditionAndNullMessage_ShouldThrowExceptionWithNullMessage() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> AssertUtils.isTrue(false, null)
        );
        assertNull(exception.getMessage());
    }

    @Test
    void isTrue_WithTrueConditionAndEmptyMessage_ShouldNotThrowException() {
        assertDoesNotThrow(() -> AssertUtils.isTrue(true, ""));
    }

    @Test
    void isTrue_WithFalseConditionAndEmptyMessage_ShouldThrowExceptionWithEmptyMessage() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> AssertUtils.isTrue(false, "")
        );
        assertEquals("", exception.getMessage());
    }
}
