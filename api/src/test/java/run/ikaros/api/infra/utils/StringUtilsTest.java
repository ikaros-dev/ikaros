package run.ikaros.api.infra.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilsTest {

    @Test
    void hasText_WithNullString_ShouldReturnFalse() {
        assertFalse(StringUtils.hasText(null));
    }

    @Test
    void hasText_WithEmptyString_ShouldReturnFalse() {
        assertFalse(StringUtils.hasText(""));
    }

    @Test
    void hasText_WithWhitespaceOnly_ShouldReturnFalse() {
        assertFalse(StringUtils.hasText("   "));
        assertFalse(StringUtils.hasText("\t\n"));
    }

    @Test
    void hasText_WithNonWhitespaceString_ShouldReturnTrue() {
        assertTrue(StringUtils.hasText("test"));
        assertTrue(StringUtils.hasText("  test  "));
        assertTrue(StringUtils.hasText("a"));
    }

    @Test
    void hasText_WithMixedContent_ShouldReturnTrue() {
        assertTrue(StringUtils.hasText("  hello world  "));
        assertTrue(StringUtils.hasText("\ttest\n"));
    }

    @Test
    void hasText_WithSpecialCharacters_ShouldReturnTrue() {
        assertTrue(StringUtils.hasText("!@#$%"));
        assertTrue(StringUtils.hasText("  中文测试  "));
    }
}