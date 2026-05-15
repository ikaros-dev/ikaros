package run.ikaros.server.infra.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class SqlUtilsTest {

    @Test
    void escapeLikeSpecialChars_nullInput() {
        assertNull(SqlUtils.escapeLikeSpecialChars(null));
    }

    @Test
    void escapeLikeSpecialChars_emptyString() {
        assertEquals("", SqlUtils.escapeLikeSpecialChars(""));
    }

    @Test
    void escapeLikeSpecialChars_noSpecialChars() {
        assertEquals("hello\\ world", SqlUtils.escapeLikeSpecialChars("hello world"));
    }

    @Test
    void escapeLikeSpecialChars_percentSign() {
        assertEquals("100\\%", SqlUtils.escapeLikeSpecialChars("100%"));
    }

    @Test
    void escapeLikeSpecialChars_underscore() {
        assertEquals("test\\_value", SqlUtils.escapeLikeSpecialChars("test_value"));
    }

    @Test
    void escapeLikeSpecialChars_backslash() {
        assertEquals("path\\\\to\\\\file", SqlUtils.escapeLikeSpecialChars("path\\to\\file"));
    }

    @Test
    void escapeLikeSpecialChars_brackets() {
        assertEquals("\\[test\\]", SqlUtils.escapeLikeSpecialChars("[test]"));
    }

    @Test
    void escapeLikeSpecialChars_singleQuote() {
        assertEquals("it''s", SqlUtils.escapeLikeSpecialChars("it's"));
    }

    @Test
    void escapeLikeSpecialChars_multipleSpecialChars() {
        assertEquals("test\\%\\_\\[value\\]", SqlUtils.escapeLikeSpecialChars("test%_[value]"));
    }

    @Test
    void escapeLikeSpecialChars_specialCharsAtEnd() {
        assertEquals("hello\\!", SqlUtils.escapeLikeSpecialChars("hello!"));
    }
}
