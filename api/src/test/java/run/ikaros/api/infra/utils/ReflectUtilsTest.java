package run.ikaros.api.infra.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ReflectUtilsTest {

    @Test
    void convertToCamelCase_withStandardField() {
        String result = ReflectUtils.convertToCamelCase("ATTACHMENT_ID");
        assertEquals("attachmentId", result);
    }

    @Test
    void convertToCamelCase_withSingleUnderscore() {
        String result = ReflectUtils.convertToCamelCase("ACTIVE");
        assertEquals("active", result);
    }

    @Test
    void convertToCamelCase_withSingleChar() {
        String result = ReflectUtils.convertToCamelCase("X");
        assertEquals("x", result);
    }

    @Test
    void convertToCamelCase_withNull_returnsNull() {
        String result = ReflectUtils.convertToCamelCase(null);
        assertEquals(null, result);
    }

    @Test
    void convertToCamelCase_withEmpty_returnsEmpty() {
        String result = ReflectUtils.convertToCamelCase("");
        assertEquals("", result);
    }

    @Test
    void convertToCamelCase_withMultiWord() {
        String result = ReflectUtils.convertToCamelCase("FIRST_NAME");
        assertEquals("firstName", result);
    }

    @Test
    void convertToCamelCase_withTrailingUnderscore() {
        // trailing underscore is a no-op (no char to capitalize after)
        String result = ReflectUtils.convertToCamelCase("USER_NAME_");
        assertEquals("userName", result);
    }

    @Test
    void convertToCamelCase_withLeadingUnderscore() {
        String result = ReflectUtils.convertToCamelCase("_ID");
        assertEquals("Id", result);
    }

    @Test
    void convertToCamelCase_withMultipleUnderscores() {
        // consecutive underscores — second flag keeps toUpperCase=true
        String result = ReflectUtils.convertToCamelCase("USER__NAME");
        assertEquals("userName", result);
    }

    @Test
    void convertToCamelCase_withNumber() {
        String result = ReflectUtils.convertToCamelCase("ID_1");
        assertEquals("id1", result);
    }
}
