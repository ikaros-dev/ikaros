package run.ikaros.api.infra.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StringUtilsTest {

    @Test
    void isBlank_withNullString_shouldReturnTrue() {
        assertTrue(StringUtils.isBlank(null));
    }

    @Test
    void isBlank_withEmptyString_shouldReturnTrue() {
        assertTrue(StringUtils.isBlank(""));
    }

    @Test
    void isBlank_withWhitespaceOnly_shouldReturnTrue() {
        assertTrue(StringUtils.isBlank("   "));
        assertTrue(StringUtils.isBlank("\t\n"));
    }

    @Test
    void isBlank_withNonWhitespaceString_shouldReturnFalse() {
        assertFalse(StringUtils.isBlank("test"));
        assertFalse(StringUtils.isBlank("  test  "));
        assertFalse(StringUtils.isBlank("a"));
    }

    @Test
    void isBlank_withMixedContent_shouldReturnFalse() {
        assertFalse(StringUtils.isBlank("  hello world  "));
        assertFalse(StringUtils.isBlank("\ttest\n"));
    }

    @Test
    void isBlank_withSpecialCharacters_shouldReturnFalse() {
        assertFalse(StringUtils.isBlank("!@#$%"));
        assertFalse(StringUtils.isBlank("  中文测试  "));
    }

    @Test
    void isNotBlank_withNonBlank_shouldReturnTrue() {
        assertTrue(StringUtils.isNotBlank("test"));
    }

    @Test
    void isNotBlank_withBlank_shouldReturnFalse() {
        assertFalse(StringUtils.isNotBlank(""));
    }

    @Test
    void upperCaseFirst_shouldCapitalizeFirstChar() {
        assertEquals("Hello", StringUtils.upperCaseFirst("hello"));
    }

    @Test
    void upperCaseFirst_withSingleChar_shouldCapitalize() {
        assertEquals("A", StringUtils.upperCaseFirst("a"));
    }
}
