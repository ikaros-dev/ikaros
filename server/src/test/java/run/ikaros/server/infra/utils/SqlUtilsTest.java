package run.ikaros.server.infra.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class SqlUtilsTest {

    @Test
    void escapeLikeSpecialCharsWithBackslash() {
        assertEquals("\\\\", SqlUtils.escapeLikeSpecialChars("\\"));
    }

    @Test
    void escapeLikeSpecialCharsWithUnderscore() {
        assertEquals("\\_", SqlUtils.escapeLikeSpecialChars("_"));
    }

    @Test
    void escapeLikeSpecialCharsWithPercent() {
        assertEquals("\\%", SqlUtils.escapeLikeSpecialChars("%"));
    }

    @Test
    void escapeLikeSpecialCharsWithBrackets() {
        assertEquals("\\[\\]", SqlUtils.escapeLikeSpecialChars("[]"));
    }

    @Test
    void escapeLikeSpecialCharsWithNull() {
        assertNull(SqlUtils.escapeLikeSpecialChars(null));
    }

    @Test
    void escapeLikeSpecialCharsWithNormalString() {
        assertEquals("hello", SqlUtils.escapeLikeSpecialChars("hello"));
    }
}
