package run.ikaros.server.infra.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ByteArrUtilsTest {

    @Test
    void isBinaryData_withTextData() {
        byte[] text = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        assertFalse(ByteArrUtils.isBinaryData(text));
    }

    @Test
    void isBinaryData_withBinaryData() {
        byte[] binary = new byte[]{0, 1, 2, 3, 4, 5};
        assertTrue(ByteArrUtils.isBinaryData(binary));
    }

    @Test
    void isBinaryData_withEmptyArray() {
        assertFalse(ByteArrUtils.isBinaryData(new byte[0]));
    }

    @Test
    void isBinaryData_withTabAndNewline() {
        byte[] text = "Hello\tWorld\n".getBytes(StandardCharsets.UTF_8);
        assertFalse(ByteArrUtils.isBinaryData(text));
    }

    @Test
    void isBinaryData_withNullByte() {
        byte[] binary = new byte[]{0};
        assertTrue(ByteArrUtils.isBinaryData(binary));
    }

    @Test
    void isBinaryData_withCarriageReturn() {
        byte[] text = "Hello\rWorld".getBytes(StandardCharsets.UTF_8);
        assertFalse(ByteArrUtils.isBinaryData(text));
    }

    @Test
    void isBinaryData_withOnlyControlChars() {
        byte[] binary = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        assertTrue(ByteArrUtils.isBinaryData(binary));
    }

    @Test
    void isBinaryData_withMixedControlAndPrintable() {
        byte[] data = new byte[]{'H', 0, 'i'};
        assertTrue(ByteArrUtils.isBinaryData(data));
    }

    @Test
    void isStringData_withValidText() {
        byte[] text = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        assertTrue(ByteArrUtils.isStringData(text));
    }

    @Test
    void isStringData_withBinaryData() {
        byte[] binary = new byte[]{0, 1, 2, 3};
        assertFalse(ByteArrUtils.isStringData(binary));
    }

    @Test
    void isStringData_withEmptyArray() {
        assertTrue(ByteArrUtils.isStringData(new byte[0]));
    }

    @Test
    void isStringData_withUnicodeText() {
        byte[] text = "你好世界".getBytes(StandardCharsets.UTF_8);
        assertTrue(ByteArrUtils.isStringData(text));
    }

    @Test
    void isStringData_withControlCharsInUtf8() {
        // Tab, LF, CR are valid control chars in string data
        byte[] text = "hello\tworld\nline\rbreak".getBytes(StandardCharsets.UTF_8);
        assertTrue(ByteArrUtils.isStringData(text));
    }

    @Test
    void isStringData_withNullByte() {
        byte[] data = new byte[]{0};
        assertFalse(ByteArrUtils.isStringData(data));
    }

    @Test
    void isStringData_withHighByteValues() {
        // Characters with values >= 32 are considered printable
        byte[] data = new byte[]{65, 66, 67}; // "ABC"
        assertTrue(ByteArrUtils.isStringData(data));
    }
}
