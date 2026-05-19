package run.ikaros.server.infra.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ByteArrUtilsTest {

    @Test
    void isBinaryDataWithControlChars() {
        byte[] data = {0, 1, 2, 127};
        assertTrue(ByteArrUtils.isBinaryData(data));
    }

    @Test
    void isBinaryDataWithTextBytes() {
        byte[] data = "Hello, world!".getBytes(StandardCharsets.UTF_8);
        assertFalse(ByteArrUtils.isBinaryData(data));
    }

    @Test
    void isBinaryDataWithEmptyArray() {
        byte[] data = new byte[0];
        assertFalse(ByteArrUtils.isBinaryData(data));
    }

    @Test
    void isStringDataWithValidUtf8Text() {
        byte[] data = "Hello, world!".getBytes(StandardCharsets.UTF_8);
        assertTrue(ByteArrUtils.isStringData(data));
    }

    @Test
    void isStringDataWithBinaryData() {
        byte[] data = {0, 1, 2, (byte) 0xFF};
        assertFalse(ByteArrUtils.isStringData(data));
    }

    @Test
    void isStringDataWithEmptyArray() {
        byte[] data = new byte[0];
        assertTrue(ByteArrUtils.isStringData(data));
    }
}
