package run.ikaros.api.infra.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReflectUtilsTest {

    @Test
    void findMatchFieldGetterMethodName_WithValidFieldName_ShouldReturnGetterName() {
        String result = ReflectUtils.findMatchFieldGetterMethodName("name");
        assertEquals("getName", result);
    }

    @Test
    void findMatchFieldGetterMethodName_WithBooleanField_ShouldReturnGetterName() {
        String result = ReflectUtils.findMatchFieldGetterMethodName("active");
        assertEquals("getActive", result);
    }

    @Test
    void findMatchFieldGetterMethodName_WithSingleCharField_ShouldReturnGetterName() {
        String result = ReflectUtils.findMatchFieldGetterMethodName("x");
        assertEquals("getX", result);
    }

    @Test
    void findMatchFieldGetterMethodName_WithNullFieldName_ShouldThrowException() {
        assertThrows(NullPointerException.class, 
            () -> ReflectUtils.findMatchFieldGetterMethodName(null));
    }

    @Test
    void findMatchFieldGetterMethodName_WithEmptyFieldName_ShouldReturnGet() {
        String result = ReflectUtils.findMatchFieldGetterMethodName("");
        assertEquals("get", result);
    }

    @Test
    void findMatchFieldGetterMethodName_WithMultiWordField_ShouldReturnGetterName() {
        String result = ReflectUtils.findMatchFieldGetterMethodName("firstName");
        assertEquals("getFirstName", result);
    }

    @Test
    void findMatchFieldGetterMethodName_WithUnderscoreField_ShouldReturnGetterName() {
        String result = ReflectUtils.findMatchFieldGetterMethodName("user_name");
        assertEquals("getUser_name", result);
    }

    @Test
    void findMatchFieldGetterMethodName_WithNumberField_ShouldReturnGetterName() {
        String result = ReflectUtils.findMatchFieldGetterMethodName("id1");
        assertEquals("getId1", result);
    }
}