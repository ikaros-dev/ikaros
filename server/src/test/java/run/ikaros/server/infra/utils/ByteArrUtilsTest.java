package run.ikaros.server.infra.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ByteArrUtilsTest {

    @Test
    void shouldDetectBinaryData() {
        byte[] binaryData = {0x00, 0x01, 0x02, 0x03};
        assertThat(ByteArrUtils.isBinaryData(binaryData)).isTrue();
    }

    @Test
    void shouldNotDetectStringAsBinary() {
        byte[] stringData = "Hello World".getBytes(StandardCharsets.UTF_8);
        assertThat(ByteArrUtils.isBinaryData(stringData)).isFalse();
    }

    @Test
    void shouldDetectControlCharsAsBinary() {
        byte[] controlChars = {0x01, 0x02, 0x03};
        assertThat(ByteArrUtils.isBinaryData(controlChars)).isTrue();
    }

    @Test
    void shouldNotDetectTabAsBinary() {
        byte[] tabData = "Hello\tWorld".getBytes(StandardCharsets.UTF_8);
        assertThat(ByteArrUtils.isBinaryData(tabData)).isFalse();
    }

    @Test
    void shouldNotDetectNewlineAsBinary() {
        byte[] newlineData = "Hello\nWorld".getBytes(StandardCharsets.UTF_8);
        assertThat(ByteArrUtils.isBinaryData(newlineData)).isFalse();
    }

    @Test
    void shouldNotDetectCarriageReturnAsBinary() {
        byte[] crData = "Hello\rWorld".getBytes(StandardCharsets.UTF_8);
        assertThat(ByteArrUtils.isBinaryData(crData)).isFalse();
    }

    @Test
    void shouldDetectStringData() {
        byte[] stringData = "Hello World".getBytes(StandardCharsets.UTF_8);
        assertThat(ByteArrUtils.isStringData(stringData)).isTrue();
    }

    @Test
    void shouldNotDetectBinaryAsString() {
        byte[] binaryData = {0x00, 0x01, 0x02, 0x03};
        assertThat(ByteArrUtils.isStringData(binaryData)).isFalse();
    }

    @Test
    void shouldDetectStringWithTab() {
        byte[] tabData = "Hello\tWorld".getBytes(StandardCharsets.UTF_8);
        assertThat(ByteArrUtils.isStringData(tabData)).isTrue();
    }

    @Test
    void shouldDetectStringWithNewline() {
        byte[] newlineData = "Hello\nWorld".getBytes(StandardCharsets.UTF_8);
        assertThat(ByteArrUtils.isStringData(newlineData)).isTrue();
    }

    @Test
    void shouldDetectStringWithCarriageReturn() {
        byte[] crData = "Hello\rWorld".getBytes(StandardCharsets.UTF_8);
        assertThat(ByteArrUtils.isStringData(crData)).isTrue();
    }

    @Test
    void shouldDetectEmptyStringAsData() {
        byte[] emptyData = "".getBytes(StandardCharsets.UTF_8);
        assertThat(ByteArrUtils.isStringData(emptyData)).isTrue();
    }

    @Test
    void shouldHandleChineseCharacters() {
        byte[] chineseData = "你好世界".getBytes(StandardCharsets.UTF_8);
        assertThat(ByteArrUtils.isStringData(chineseData)).isTrue();
    }
}
