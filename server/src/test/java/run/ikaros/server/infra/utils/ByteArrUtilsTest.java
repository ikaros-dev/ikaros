package run.ikaros.server.infra.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ByteArrUtilsTest {

    @Test
    void isBinaryData_pngHeader_returnsTrue() {
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47};
        assertThat(ByteArrUtils.isBinaryData(png)).isTrue();
    }

    @Test
    void isBinaryData_textAscii_returnsFalse() {
        byte[] text = "Hello World".getBytes();
        assertThat(ByteArrUtils.isBinaryData(text)).isFalse();
    }

    @Test
    void isBinaryData_empty_returnsFalse() {
        assertThat(ByteArrUtils.isBinaryData(new byte[0])).isFalse();
    }

    @Test
    void isBinaryData_withNullByte_returnsTrue() {
        byte[] data = {0x48, 0x00, 0x65, 0x6C};
        assertThat(ByteArrUtils.isBinaryData(data)).isTrue();
    }

    @Test
    void isBinaryData_withControlChars_returnsTrue() {
        byte[] data = {0x01, 0x02, 0x03};
        assertThat(ByteArrUtils.isBinaryData(data)).isTrue();
    }

    @Test
    void isBinaryData_tabNewlineReturn_returnsFalse() {
        byte[] data = {0x09, 0x0A, 0x0D, 0x48};
        assertThat(ByteArrUtils.isBinaryData(data)).isFalse();
    }

    @Test
    void isBinaryData_null_throwsNpe() {
        assertThrows(NullPointerException.class, () -> ByteArrUtils.isBinaryData(null));
    }

    @Test
    void isStringData_text_returnsTrue() {
        byte[] text = "plain text".getBytes();
        assertThat(ByteArrUtils.isStringData(text)).isTrue();
    }

    @Test
    void isStringData_null_returnsFalse() {
        assertThat(ByteArrUtils.isStringData(null)).isFalse();
    }

    @Test
    void isStringData_empty_returnsTrue() {
        assertThat(ByteArrUtils.isStringData(new byte[0])).isTrue();
    }
}
