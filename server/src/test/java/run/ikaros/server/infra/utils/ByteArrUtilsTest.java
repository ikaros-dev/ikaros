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
}
