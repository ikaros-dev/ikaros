package run.ikaros.api.core.media;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/**
 * 有界媒体真实格式检测器测试.
 */
class MediaFileDetectorTest {

    @Test
    void shouldDetectImageFormatsAndReturnCanonicalMime() {
        assertDetected(png(), "png", MediaFileFormat.PNG);
        assertDetected(jpeg(), "png", MediaFileFormat.JPEG);
        assertDetected(gif(), "gif", MediaFileFormat.GIF);
        assertDetected(webp(), "webp", MediaFileFormat.WEBP);
        assertDetected(bmp(), "bmp", MediaFileFormat.BMP);
        assertDetected(bmff("avif", "mif1"), "avif", MediaFileFormat.AVIF);
        assertDetected(bmff("heic", "mif1"), "heic", MediaFileFormat.HEIF);
        assertDetected(text("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1\" "
            + "height=\"1\"><path d=\"M0 0\"/></svg>"), "svg", MediaFileFormat.SVG);
    }

    @Test
    void shouldDetectVideoContainers() {
        assertDetected(bmff("isom", "mp42"), "mp4", MediaFileFormat.MP4);
        assertDetected(bmff("qt  ", "qt  "), "mov", MediaFileFormat.QUICKTIME);
        assertDetected(ebml("matroska"), "mp4", MediaFileFormat.MATROSKA);
        assertDetected(ebml("webm"), "webm", MediaFileFormat.WEBM);
        assertDetected(avi(), "avi", MediaFileFormat.AVI);
        assertDetected(flv(), "flv", MediaFileFormat.FLV);
        assertDetected(mpegTs(0), "ts", MediaFileFormat.MPEG_TS);
        assertDetected(mpegTs(4), "m2ts", MediaFileFormat.MPEG_TS);
        assertDetected(asf(false), "wmv", MediaFileFormat.WMV);
    }

    @Test
    void shouldDetectAudioFormats() {
        assertDetected(bmff("M4A ", "isom"), "m4a", MediaFileFormat.M4A);
        assertDetected(mp3(), "mp4", MediaFileFormat.MP3);
        assertDetected(aac(), "aac", MediaFileFormat.AAC);
        assertDetected(flac(), "flac", MediaFileFormat.FLAC);
        assertDetected(ogg(vorbisPacket()), "ogg", MediaFileFormat.OGG);
        assertDetected(ogg(opusPacket()), "opus", MediaFileFormat.OPUS);
        assertDetected(wav(), "wav", MediaFileFormat.WAV);
        assertDetected(asf(true), "wma", MediaFileFormat.WMA);
    }

    @Test
    void shouldDetectStrictSubtitleAndLyricsFormats() {
        assertDetected(text("1\n00:00:01,000 --> 00:00:02,250\nHello\n"),
            "srt", MediaFileFormat.SRT);
        assertDetected(text("[Script Info]\nScriptType: v4.00+\n[Events]\n"
            + "Format: Layer, Start, End, Style, Text\n"
            + "Dialogue: 0,0:00:01.00,0:00:02.00,Default,Hello\n"),
            "ass", MediaFileFormat.ASS);
        assertDetected(text("[Script Info]\nScriptType: v4.00\n[Events]\n"
            + "Format: Marked, Start, End, Style, Text\n"
            + "Dialogue: Marked=0,0:00:01.00,0:00:02.00,Default,Hello\n"),
            "ssa", MediaFileFormat.SSA);
        assertDetected(text("\uFEFF  WEBVTT\n\n00:01.000 --> 00:02.000\nHello\n"),
            "vtt", MediaFileFormat.VTT);
        assertDetected(text("[00:12.34]Hello\n"), "lrc", MediaFileFormat.LRC);
        assertDetected(text("{1}{24}Hello\n"), "sub", MediaFileFormat.MICRODVD);
        assertDetected(vobSub(), "sub", MediaFileFormat.VOBSUB);
        assertDetected(text("# VobSub index file, v7\n"
            + "timestamp: 00:01:02:003, filepos: 00000000\n"), "idx", MediaFileFormat.IDX);
        assertDetected(text("<tt xmlns=\"http://www.w3.org/ns/ttml\"><body><div>"
            + "<p begin=\"00:00:01.000\" end=\"00:00:02.000\">Hello</p>"
            + "</div></body></tt>"), "ttml", MediaFileFormat.TTML);
    }

    @Test
    void shouldRejectTextFormatMismatchesAndUnsafeText() {
        byte[] srt = text("1\n00:00:01,000 --> 00:00:02,000\nHello\n");
        byte[] ass = text("[Script Info]\nScriptType: v4.00+\n[Events]\n"
            + "Format: Layer, Start, End, Text\nDialogue: 0,0:00:01.00,0:00:02.00,Hello\n");
        byte[] lrc = text("[00:01.00]Hello\n");

        assertThat(MediaFileDetector.detect(srt, "ass")).isEmpty();
        assertThat(MediaFileDetector.detect(ass, "srt")).isEmpty();
        assertThat(MediaFileDetector.detect(lrc, "srt")).isEmpty();
        assertThat(MediaFileDetector.detect(srt, "lrc")).isEmpty();
        assertThat(MediaFileDetector.detect(png(), "srt")).isEmpty();
        assertThat(MediaFileDetector.detect(text("plain text"), "srt")).isEmpty();
        assertThat(MediaFileDetector.detect(text("# VobSub index file, v7"), "idx")).isEmpty();
        assertThat(MediaFileDetector.detect(text("ordinary subtitle"), "sub")).isEmpty();
    }

    @Test
    void shouldRejectDoctypeAndInsufficientTtml() {
        byte[] xxe = text("<?xml version=\"1.0\"?><!DOCTYPE tt [<!ENTITY xxe SYSTEM "
            + "\"file:///etc/passwd\">]><tt xmlns=\"http://www.w3.org/ns/ttml\">"
            + "<body><div><p begin=\"1s\">&xxe;</p></div></body></tt>");
        byte[] insufficient = text("<tt xmlns=\"http://www.w3.org/ns/ttml\"><body/></tt>");

        assertThat(MediaFileDetector.detect(xxe, "ttml")).isEmpty();
        assertThat(MediaFileDetector.detect(insufficient, "ttml")).isEmpty();
    }

    @Test
    void shouldRejectExecutablesArchivesTruncationAndForgedHeaders() {
        byte[] pe = new byte[128];
        pe[0] = 'M';
        pe[1] = 'Z';
        putIntLe(pe, 0x3c, 64);
        copy(pe, 64, "PE\0\0".getBytes(StandardCharsets.US_ASCII));

        assertThat(MediaFileDetector.detect(pe, "mp4")).isEmpty();
        assertThat(MediaFileDetector.detect(hex("504b030414000000"), "mp4")).isEmpty();
        assertThat(MediaFileDetector.detect(hex("89504e47"), "png")).isEmpty();
        assertThat(MediaFileDetector.detect(hex("ffd8ffc00011"), "jpg")).isEmpty();
        assertThat(MediaFileDetector.detect(hex("fffb9064"), "mp3")).isEmpty();
        assertThat(MediaFileDetector.detect(new byte[]{0x47, 0, 0, 0x10}, "ts")).isEmpty();
        assertThat(MediaFileDetector.detect(bmff("zzzz", "none"), "mp4")).isEmpty();
        assertThat(MediaFileDetector.detect(hex("000000006674797069736f6d00000000"), "mp4"))
            .isEmpty();
        assertThat(MediaFileDetector.detect(text("unknown content"), "mp4")).isEmpty();
        assertThat(MediaFileDetector.detect(png(), "exe")).isEmpty();
        assertThat(MediaFileDetector.detect(new byte[MediaFileDetector.MAX_PREFIX_SIZE + 1], "mp4"))
            .isEmpty();
    }

    private void assertDetected(byte[] bytes, String extension, MediaFileFormat expected) {
        assertThat(MediaFileDetector.detect(bytes, extension))
            .hasValueSatisfying(result -> {
                assertThat(result.format()).isEqualTo(expected);
                assertThat(result.category()).isEqualTo(expected.category());
                assertThat(result.mimeType()).isEqualTo(expected.mimeType());
            });
    }

    private byte[] png() {
        byte[] data = new byte[33];
        copy(data, 0, hex("89504e470d0a1a0a"));
        putIntBe(data, 8, 13);
        copy(data, 12, text("IHDR"));
        putIntBe(data, 16, 1);
        putIntBe(data, 20, 1);
        return data;
    }

    private byte[] jpeg() {
        return hex("ffd8ffc00011080001000103011100021100031100");
    }

    private byte[] gif() {
        byte[] data = new byte[13];
        copy(data, 0, text("GIF89a"));
        putShortLe(data, 6, 1);
        putShortLe(data, 8, 1);
        return data;
    }

    private byte[] webp() {
        byte[] data = new byte[30];
        copy(data, 0, text("RIFF"));
        putIntLe(data, 4, 22);
        copy(data, 8, text("WEBPVP8X"));
        putIntLe(data, 16, 10);
        return data;
    }

    private byte[] bmp() {
        byte[] data = new byte[30];
        copy(data, 0, text("BM"));
        putIntLe(data, 2, 30);
        putIntLe(data, 10, 26);
        putIntLe(data, 14, 12);
        putShortLe(data, 18, 1);
        putShortLe(data, 20, 1);
        putShortLe(data, 22, 1);
        putShortLe(data, 24, 24);
        return data;
    }

    private byte[] bmff(String majorBrand, String compatibleBrand) {
        byte[] data = new byte[20];
        putIntBe(data, 0, data.length);
        copy(data, 4, text("ftyp"));
        copy(data, 8, text(majorBrand));
        copy(data, 16, text(compatibleBrand));
        return data;
    }

    private byte[] ebml(String documentType) {
        byte[] type = text(documentType);
        int payloadLength = 3 + type.length;
        byte[] data = new byte[5 + payloadLength];
        copy(data, 0, hex("1a45dfa3"));
        data[4] = (byte) (0x80 | payloadLength);
        data[5] = 0x42;
        data[6] = (byte) 0x82;
        data[7] = (byte) (0x80 | type.length);
        copy(data, 8, type);
        return data;
    }

    private byte[] avi() {
        byte[] data = new byte[24];
        copy(data, 0, text("RIFF"));
        putIntLe(data, 4, 16);
        copy(data, 8, text("AVI LIST"));
        putIntLe(data, 16, 4);
        copy(data, 20, text("hdrl"));
        return data;
    }

    private byte[] wav() {
        byte[] data = new byte[36];
        copy(data, 0, text("RIFF"));
        putIntLe(data, 4, 28);
        copy(data, 8, text("WAVEfmt "));
        putIntLe(data, 16, 16);
        putShortLe(data, 20, 1);
        return data;
    }

    private byte[] flac() {
        byte[] data = new byte[42];
        copy(data, 0, text("fLaC"));
        data[4] = (byte) 0x80;
        data[7] = 34;
        return data;
    }

    private byte[] ogg(byte[] packet) {
        byte[] data = new byte[28 + packet.length];
        copy(data, 0, text("OggS"));
        data[5] = 2;
        data[26] = 1;
        data[27] = (byte) packet.length;
        copy(data, 28, packet);
        return data;
    }

    private byte[] vorbisPacket() {
        byte[] packet = new byte[30];
        packet[0] = 1;
        copy(packet, 1, text("vorbis"));
        packet[11] = 2;
        putIntLe(packet, 12, 48000);
        packet[28] = (byte) 0xb8;
        packet[29] = 1;
        return packet;
    }

    private byte[] opusPacket() {
        byte[] packet = new byte[19];
        copy(packet, 0, text("OpusHead"));
        packet[8] = 1;
        packet[9] = 2;
        putIntLe(packet, 12, 48000);
        return packet;
    }

    private byte[] mp3() {
        byte[] data = new byte[417];
        copy(data, 0, hex("fffb9064"));
        return data;
    }

    private byte[] aac() {
        byte[] data = new byte[9];
        copy(data, 0, hex("fff15080013ffc"));
        return data;
    }

    private byte[] asf(boolean audio) {
        byte[] data = new byte[70];
        copy(data, 0, hex("3026b2758e66cf11a6d900aa0062ce6c"));
        putLongLe(data, 16, data.length);
        putIntLe(data, 24, 1);
        copy(data, 30, hex("9107dcb7b7a9cf118ee600c00c205365"));
        putLongLe(data, 46, 40);
        copy(data, 54, audio
            ? hex("409e69f84d5bcf11a8fd00805f5c442b")
            : hex("c0ef19bc4d5bcf11a8fd00805f5c442b"));
        return data;
    }

    private byte[] flv() {
        byte[] data = new byte[25];
        copy(data, 0, text("FLV"));
        data[3] = 1;
        data[4] = 1;
        putIntBe(data, 5, 9);
        data[13] = 8;
        data[16] = 1;
        return data;
    }

    private byte[] mpegTs(int start) {
        byte[] data = new byte[start + 564];
        for (int offset = start; offset < data.length; offset += 188) {
            data[offset] = 0x47;
            data[offset + 3] = 0x10;
        }
        return data;
    }

    private byte[] vobSub() {
        byte[] data = new byte[24];
        copy(data, 0, hex("000001ba"));
        copy(data, 10, hex("000001bd"));
        return data;
    }

    private byte[] text(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] hex(String value) {
        return HexFormat.of().parseHex(value);
    }

    private void copy(byte[] target, int offset, byte[] source) {
        System.arraycopy(source, 0, target, offset, source.length);
    }

    private void putShortLe(byte[] target, int offset, int value) {
        ByteBuffer.wrap(target, offset, 2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) value);
    }

    private void putIntBe(byte[] target, int offset, int value) {
        ByteBuffer.wrap(target, offset, 4).putInt(value);
    }

    private void putIntLe(byte[] target, int offset, int value) {
        ByteBuffer.wrap(target, offset, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(value);
    }

    private void putLongLe(byte[] target, int offset, long value) {
        ByteBuffer.wrap(target, offset, 8).order(ByteOrder.LITTLE_ENDIAN).putLong(value);
    }
}
