package run.ikaros.api.core.media;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 仅使用固定大小字节前缀确认媒体文件真实格式的无状态检测器。
 */
public final class MediaFileDetector {

    /** 检测器允许消费的最大前缀长度。 */
    public static final int MAX_PREFIX_SIZE = 64 * 1024;

    /** SRT 序号、时间轴和正文结构。 */
    private static final Pattern SRT_PATTERN = Pattern.compile(
        "(?ms)^\\s*\\d+\\s*\\R\\d{2}:\\d{2}:\\d{2},\\d{3}\\s+-->\\s+"
            + "\\d{2}:\\d{2}:\\d{2},\\d{3}[^\\r\\n]*\\R\\S.*");

    /** WebVTT 点号毫秒时间轴。 */
    private static final Pattern VTT_TIMELINE = Pattern.compile(
        "(?m)^(?:\\S+\\R)?(?:\\d{2}:)?\\d{2}:\\d{2}\\.\\d{3}\\s+-->\\s+"
            + "(?:\\d{2}:)?\\d{2}:\\d{2}\\.\\d{3}(?:\\s+.*)?$");

    /** LRC 时间标签。 */
    private static final Pattern LRC_TIMELINE = Pattern.compile(
        "(?m)^.*\\[\\d{1,3}:\\d{2}\\.\\d{2,3}].+$");

    /** MicroDVD 帧时间轴。 */
    private static final Pattern MICRODVD_TIMELINE = Pattern.compile(
        "(?m)^\\{\\d+}\\{\\d+}.+$");

    /** VobSub IDX 时间戳和文件位置。 */
    private static final Pattern IDX_TIMELINE = Pattern.compile(
        "(?im)^timestamp:\\s*\\d{2}:\\d{2}:\\d{2}:\\d{3},\\s*filepos:\\s*[0-9a-f]{8,16}\\s*$");

    /** ASF Header Object GUID。 */
    private static final byte[] ASF_HEADER_GUID = hex(
        "3026b2758e66cf11a6d900aa0062ce6c");

    /** ASF Stream Properties Object GUID。 */
    private static final byte[] ASF_STREAM_PROPERTIES_GUID = hex(
        "9107dcb7b7a9cf118ee600c00c205365");

    /** ASF Audio Media GUID。 */
    private static final byte[] ASF_AUDIO_GUID = hex(
        "409e69f84d5bcf11a8fd00805f5c442b");

    /** ASF Video Media GUID。 */
    private static final byte[] ASF_VIDEO_GUID = hex(
        "c0ef19bc4d5bcf11a8fd00805f5c442b");

    private MediaFileDetector() {
    }

    public static Optional<MediaFileDetectionResult> detect(byte[] prefix, String extension) {
        if (prefix == null || prefix.length == 0 || prefix.length > MAX_PREFIX_SIZE
            || MediaFilePolicy.formatsForExtension(extension).isEmpty()) {
            return Optional.empty();
        }
        MediaFileFormat format = detectBinary(prefix).orElseGet(() -> detectText(prefix, extension).orElse(null));
        if (format == null) {
            return Optional.empty();
        }
        MediaFileDetectionResult result = new MediaFileDetectionResult(format);
        return MediaFilePolicy.isDetectionAllowed(extension, result) ? Optional.of(result) : Optional.empty();
    }

    private static Optional<MediaFileFormat> detectBinary(byte[] data) {
        if (isPe(data) || isZip(data)) {
            return Optional.empty();
        }
        if (isPng(data)) {
            return Optional.of(MediaFileFormat.PNG);
        }
        if (isJpeg(data)) {
            return Optional.of(MediaFileFormat.JPEG);
        }
        if (isGif(data)) {
            return Optional.of(MediaFileFormat.GIF);
        }
        if (isWebp(data)) {
            return Optional.of(MediaFileFormat.WEBP);
        }
        if (isBmp(data)) {
            return Optional.of(MediaFileFormat.BMP);
        }
        Optional<MediaFileFormat> bmff = detectIsoBmff(data);
        if (bmff.isPresent()) {
            return bmff;
        }
        Optional<MediaFileFormat> ebml = detectEbml(data);
        if (ebml.isPresent()) {
            return ebml;
        }
        Optional<MediaFileFormat> riff = detectRiff(data);
        if (riff.isPresent()) {
            return riff;
        }
        if (isFlac(data)) {
            return Optional.of(MediaFileFormat.FLAC);
        }
        Optional<MediaFileFormat> ogg = detectOgg(data);
        if (ogg.isPresent()) {
            return ogg;
        }
        Optional<MediaFileFormat> mpegAudio = detectMpegAudio(data);
        if (mpegAudio.isPresent()) {
            return mpegAudio;
        }
        Optional<MediaFileFormat> asf = detectAsf(data);
        if (asf.isPresent()) {
            return asf;
        }
        if (isFlv(data)) {
            return Optional.of(MediaFileFormat.FLV);
        }
        if (isMpegTs(data)) {
            return Optional.of(MediaFileFormat.MPEG_TS);
        }
        if (isVobSub(data)) {
            return Optional.of(MediaFileFormat.VOBSUB);
        }
        return Optional.empty();
    }

    private static Optional<MediaFileFormat> detectText(byte[] data, String extension) {
        Optional<String> decoded = decodeUtf8(data);
        if (decoded.isEmpty()) {
            return Optional.empty();
        }
        String text = decoded.get();
        String normalized = extension.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "srt" -> SRT_PATTERN.matcher(text).find()
                ? Optional.of(MediaFileFormat.SRT) : Optional.empty();
            case "ass" -> isAss(text, true)
                ? Optional.of(MediaFileFormat.ASS) : Optional.empty();
            case "ssa" -> isAss(text, false)
                ? Optional.of(MediaFileFormat.SSA) : Optional.empty();
            case "vtt" -> isVtt(text)
                ? Optional.of(MediaFileFormat.VTT) : Optional.empty();
            case "lrc" -> LRC_TIMELINE.matcher(text).find()
                ? Optional.of(MediaFileFormat.LRC) : Optional.empty();
            case "idx" -> isIdx(text)
                ? Optional.of(MediaFileFormat.IDX) : Optional.empty();
            case "sub" -> MICRODVD_TIMELINE.matcher(text).find()
                ? Optional.of(MediaFileFormat.MICRODVD) : Optional.empty();
            case "ttml" -> isTtml(data)
                ? Optional.of(MediaFileFormat.TTML) : Optional.empty();
            default -> Optional.empty();
        };
    }

    private static boolean isPng(byte[] data) {
        return data.length >= 33
            && startsWith(data, hex("89504e470d0a1a0a"))
            && readUInt32Be(data, 8) == 13
            && asciiEquals(data, 12, "IHDR")
            && readUInt32Be(data, 16) > 0
            && readUInt32Be(data, 20) > 0;
    }

    private static boolean isJpeg(byte[] data) {
        if (data.length < 10 || unsigned(data[0]) != 0xff || unsigned(data[1]) != 0xd8) {
            return false;
        }
        int offset = 2;
        while (offset + 3 < data.length) {
            if (unsigned(data[offset]) != 0xff) {
                return false;
            }
            while (offset < data.length && unsigned(data[offset]) == 0xff) {
                offset++;
            }
            if (offset >= data.length) {
                return false;
            }
            int marker = unsigned(data[offset++]);
            if (marker == 0xd9) {
                return false;
            }
            if (marker == 0xda) {
                return offset + 2 <= data.length;
            }
            if (marker == 0x01 || marker >= 0xd0 && marker <= 0xd7) {
                continue;
            }
            if (offset + 2 > data.length) {
                return false;
            }
            int length = readUInt16Be(data, offset);
            if (length < 2 || offset + length > data.length) {
                return false;
            }
            if (isStartOfFrame(marker) && length >= 8) {
                return readUInt16Be(data, offset + 3) > 0 && readUInt16Be(data, offset + 5) > 0;
            }
            offset += length;
        }
        return false;
    }

    private static boolean isStartOfFrame(int marker) {
        return marker >= 0xc0 && marker <= 0xcf
            && marker != 0xc4 && marker != 0xc8 && marker != 0xcc;
    }

    private static boolean isGif(byte[] data) {
        return data.length >= 13
            && (asciiEquals(data, 0, "GIF87a") || asciiEquals(data, 0, "GIF89a"))
            && readUInt16Le(data, 6) > 0 && readUInt16Le(data, 8) > 0;
    }

    private static boolean isWebp(byte[] data) {
        if (data.length < 20 || !asciiEquals(data, 0, "RIFF") || !asciiEquals(data, 8, "WEBP")) {
            return false;
        }
        long riffSize = readUInt32Le(data, 4);
        long chunkSize = readUInt32Le(data, 16);
        String chunk = ascii(data, 12, 4);
        long minimumChunkSize = switch (chunk) {
            case "VP8 ", "VP8X" -> 10;
            case "VP8L" -> 5;
            default -> Long.MAX_VALUE;
        };
        return riffSize >= 12 && riffSize + 8 >= 20 && chunkSize >= minimumChunkSize
            && chunkSize <= data.length - 20L;
    }

    private static boolean isBmp(byte[] data) {
        if (data.length < 26 || !asciiEquals(data, 0, "BM")) {
            return false;
        }
        long fileSize = readUInt32Le(data, 2);
        long pixelOffset = readUInt32Le(data, 10);
        long dibSize = readUInt32Le(data, 14);
        if (fileSize < pixelOffset || pixelOffset < 26) {
            return false;
        }
        if (dibSize == 12) {
            return readUInt16Le(data, 18) > 0 && readUInt16Le(data, 20) > 0
                && readUInt16Le(data, 22) == 1 && readUInt16Le(data, 24) > 0;
        }
        return dibSize >= 40 && data.length >= 54 && readUInt32Le(data, 18) > 0
            && readUInt32Le(data, 22) != 0 && readUInt16Le(data, 26) == 1;
    }

    private static Optional<MediaFileFormat> detectIsoBmff(byte[] data) {
        if (data.length < 16) {
            return Optional.empty();
        }
        long boxSize = readUInt32Be(data, 0);
        if (boxSize < 16 || boxSize > data.length || !asciiEquals(data, 4, "ftyp")
            || (boxSize - 16) % 4 != 0) {
            return Optional.empty();
        }
        String[] brands = new String[(int) ((boxSize - 12) / 4)];
        brands[0] = ascii(data, 8, 4);
        int index = 1;
        for (int offset = 16; offset + 4 <= boxSize; offset += 4) {
            brands[index++] = ascii(data, offset, 4);
        }
        if (containsBrand(brands, "avif", "avis")) {
            return Optional.of(MediaFileFormat.AVIF);
        }
        if (containsBrand(brands, "M4A ", "M4B ", "M4P ")) {
            return Optional.of(MediaFileFormat.M4A);
        }
        if (containsBrand(brands, "qt  ")) {
            return Optional.of(MediaFileFormat.QUICKTIME);
        }
        if (containsBrand(brands, "isom", "iso2", "iso4", "iso5", "iso6", "mp41", "mp42",
            "avc1", "dash", "MSNV", "M4V ")) {
            return Optional.of(MediaFileFormat.MP4);
        }
        return Optional.empty();
    }

    private static Optional<MediaFileFormat> detectEbml(byte[] data) {
        byte[] signature = hex("1a45dfa3");
        if (data.length < 8 || !startsWith(data, signature)) {
            return Optional.empty();
        }
        VarInt size = readEbmlVarInt(data, 4);
        if (size == null || size.value <= 0 || size.value > data.length - 4L - size.length) {
            return Optional.empty();
        }
        int end = (int) (4 + size.length + size.value);
        for (int offset = 4 + size.length; offset + 3 <= end; offset++) {
            if (unsigned(data[offset]) == 0x42 && unsigned(data[offset + 1]) == 0x82) {
                VarInt docTypeLength = readEbmlVarInt(data, offset + 2);
                if (docTypeLength == null || docTypeLength.value <= 0
                    || docTypeLength.value > end - offset - 2L - docTypeLength.length) {
                    return Optional.empty();
                }
                String docType = ascii(data, offset + 2 + docTypeLength.length,
                    (int) docTypeLength.value);
                if ("webm".equals(docType)) {
                    return Optional.of(MediaFileFormat.WEBM);
                }
                if ("matroska".equals(docType)) {
                    return Optional.of(MediaFileFormat.MATROSKA);
                }
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static Optional<MediaFileFormat> detectRiff(byte[] data) {
        if (data.length < 20 || !asciiEquals(data, 0, "RIFF") || readUInt32Le(data, 4) < 12) {
            return Optional.empty();
        }
        if (data.length >= 24 && asciiEquals(data, 8, "AVI ") && asciiEquals(data, 12, "LIST")
            && readUInt32Le(data, 16) >= 4 && asciiEquals(data, 20, "hdrl")) {
            return Optional.of(MediaFileFormat.AVI);
        }
        if (!asciiEquals(data, 8, "WAVE") || !asciiEquals(data, 12, "fmt ")) {
            return Optional.empty();
        }
        long fmtSize = readUInt32Le(data, 16);
        return fmtSize >= 16 && data.length >= 20 + fmtSize && readUInt16Le(data, 20) != 0
            ? Optional.of(MediaFileFormat.WAV) : Optional.empty();
    }

    private static boolean isFlac(byte[] data) {
        return data.length >= 42 && asciiEquals(data, 0, "fLaC")
            && (unsigned(data[4]) & 0x7f) == 0
            && readUInt24Be(data, 5) == 34;
    }

    private static Optional<MediaFileFormat> detectOgg(byte[] data) {
        if (data.length < 28 || !asciiEquals(data, 0, "OggS") || data[4] != 0
            || (unsigned(data[5]) & 0x02) == 0 || readUInt32Le(data, 18) != 0) {
            return Optional.empty();
        }
        int segmentCount = unsigned(data[26]);
        if (data.length < 27 + segmentCount || segmentCount == 0) {
            return Optional.empty();
        }
        int packetLength = 0;
        for (int i = 0; i < segmentCount; i++) {
            packetLength += unsigned(data[27 + i]);
            if (unsigned(data[27 + i]) < 255) {
                break;
            }
        }
        int packetOffset = 27 + segmentCount;
        if (packetLength <= 0 || packetOffset + packetLength > data.length) {
            return Optional.empty();
        }
        if (packetLength >= 19 && asciiEquals(data, packetOffset, "OpusHead")
            && unsigned(data[packetOffset + 8]) == 1
            && unsigned(data[packetOffset + 9]) > 0
            && readUInt32Le(data, packetOffset + 12) > 0) {
            return Optional.of(MediaFileFormat.OPUS);
        }
        if (packetLength >= 30 && unsigned(data[packetOffset]) == 1
            && asciiEquals(data, packetOffset + 1, "vorbis")
            && readUInt32Le(data, packetOffset + 7) == 0
            && unsigned(data[packetOffset + 11]) > 0
            && readUInt32Le(data, packetOffset + 12) > 0
            && (unsigned(data[packetOffset + 29]) & 1) == 1) {
            return Optional.of(MediaFileFormat.OGG);
        }
        return Optional.empty();
    }

    private static Optional<MediaFileFormat> detectMpegAudio(byte[] data) {
        int offset = 0;
        if (data.length >= 10 && asciiEquals(data, 0, "ID3")) {
            if ((data[6] | data[7] | data[8] | data[9]) < 0
                || (unsigned(data[6]) | unsigned(data[7]) | unsigned(data[8]) | unsigned(data[9])) > 127) {
                return Optional.empty();
            }
            offset = 10 + (unsigned(data[6]) << 21) + (unsigned(data[7]) << 14)
                + (unsigned(data[8]) << 7) + unsigned(data[9]);
        }
        if (offset + 7 <= data.length && isAdtsHeader(data, offset)) {
            int frameLength = (unsigned(data[offset + 3]) & 0x03) << 11
                | unsigned(data[offset + 4]) << 3 | unsigned(data[offset + 5]) >> 5;
            if (frameLength >= 7 && offset + frameLength <= data.length) {
                return Optional.of(MediaFileFormat.AAC);
            }
        }
        if (offset + 4 <= data.length && isMpegAudioHeader(data, offset)) {
            int frameLength = mpegAudioFrameLength(data, offset);
            if (frameLength > 0 && offset + frameLength <= data.length) {
                return Optional.of(MediaFileFormat.MP3);
            }
        }
        return Optional.empty();
    }

    private static boolean isAdtsHeader(byte[] data, int offset) {
        int frequencyIndex = unsigned(data[offset + 2]) >> 2 & 0x0f;
        int channelConfiguration = (unsigned(data[offset + 2]) & 1) << 2
            | unsigned(data[offset + 3]) >> 6;
        return unsigned(data[offset]) == 0xff && (unsigned(data[offset + 1]) & 0xf6) == 0xf0
            && frequencyIndex != 0x0f && channelConfiguration != 0;
    }

    private static boolean isMpegAudioHeader(byte[] data, int offset) {
        int b1 = unsigned(data[offset + 1]);
        int b2 = unsigned(data[offset + 2]);
        int version = b1 >> 3 & 0x03;
        int layer = b1 >> 1 & 0x03;
        int bitrate = b2 >> 4 & 0x0f;
        int sampleRate = b2 >> 2 & 0x03;
        return unsigned(data[offset]) == 0xff && (b1 & 0xe0) == 0xe0
            && version != 1 && layer != 0 && bitrate != 0 && bitrate != 15 && sampleRate != 3;
    }

    private static int mpegAudioFrameLength(byte[] data, int offset) {
        int b1 = unsigned(data[offset + 1]);
        int b2 = unsigned(data[offset + 2]);
        int version = b1 >> 3 & 0x03;
        int layer = b1 >> 1 & 0x03;
        int bitrateIndex = b2 >> 4 & 0x0f;
        int sampleRateIndex = b2 >> 2 & 0x03;
        int padding = b2 >> 1 & 1;
        int[] sampleRates = version == 3 ? new int[]{44100, 48000, 32000}
            : version == 2 ? new int[]{22050, 24000, 16000}
            : new int[]{11025, 12000, 8000};
        int[][] mpeg1Bitrates = {
            {},
            {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320},
            {0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384},
            {0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448}
        };
        int[][] mpeg2Bitrates = {
            {},
            {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160},
            {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160},
            {0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256}
        };
        int bitrate = (version == 3 ? mpeg1Bitrates : mpeg2Bitrates)[layer][bitrateIndex] * 1000;
        int sampleRate = sampleRates[sampleRateIndex];
        if (layer == 3) {
            return (12 * bitrate / sampleRate + padding) * 4;
        }
        int coefficient = layer == 1 && version != 3 ? 72 : 144;
        return coefficient * bitrate / sampleRate + padding;
    }

    private static Optional<MediaFileFormat> detectAsf(byte[] data) {
        if (data.length < 30 || !startsWith(data, ASF_HEADER_GUID)) {
            return Optional.empty();
        }
        long headerSize = readUInt64Le(data, 16);
        long objectCount = readUInt32Le(data, 24);
        if (headerSize < 30 || headerSize > data.length || objectCount == 0 || objectCount > 1024) {
            return Optional.empty();
        }
        int offset = 30;
        for (long i = 0; i < objectCount && offset + 24 <= headerSize; i++) {
            long objectSize = readUInt64Le(data, offset + 16);
            if (objectSize < 24 || objectSize > headerSize - offset) {
                return Optional.empty();
            }
            if (matches(data, offset, ASF_STREAM_PROPERTIES_GUID) && objectSize >= 40) {
                int streamTypeOffset = offset + 24;
                if (matches(data, streamTypeOffset, ASF_AUDIO_GUID)) {
                    return Optional.of(MediaFileFormat.WMA);
                }
                if (matches(data, streamTypeOffset, ASF_VIDEO_GUID)) {
                    return Optional.of(MediaFileFormat.WMV);
                }
                return Optional.empty();
            }
            offset += (int) objectSize;
        }
        return Optional.empty();
    }

    private static boolean isFlv(byte[] data) {
        if (data.length < 24 || !asciiEquals(data, 0, "FLV") || data[3] != 1
            || (unsigned(data[4]) & 0xfa) != 0 || (unsigned(data[4]) & 0x05) == 0) {
            return false;
        }
        long dataOffset = readUInt32Be(data, 5);
        if (dataOffset < 9 || dataOffset + 15 > data.length || readUInt32Be(data, (int) dataOffset) != 0) {
            return false;
        }
        int tagType = unsigned(data[(int) dataOffset + 4]);
        int tagSize = readUInt24Be(data, (int) dataOffset + 5);
        return (tagType == 8 || tagType == 9 || tagType == 18)
            && dataOffset + 15L + tagSize <= data.length;
    }

    private static boolean isMpegTs(byte[] data) {
        for (int start : new int[]{0, 4}) {
            if (data.length >= start + 377 && validTsPacket(data, start)
                && validTsPacket(data, start + 188) && validTsPacket(data, start + 376)) {
                return true;
            }
        }
        return false;
    }

    private static boolean validTsPacket(byte[] data, int offset) {
        return unsigned(data[offset]) == 0x47 && (unsigned(data[offset + 1]) & 0x80) == 0
            && (unsigned(data[offset + 3]) & 0x30) != 0;
    }

    private static boolean isVobSub(byte[] data) {
        if (data.length < 24 || !startsWith(data, hex("000001ba"))) {
            return false;
        }
        for (int i = 4; i + 4 <= data.length; i++) {
            if (data[i] == 0 && data[i + 1] == 0 && data[i + 2] == 1
                && unsigned(data[i + 3]) == 0xbd) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAss(String text, boolean ass) {
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String scriptType = ass ? "(?im)^ScriptType:\\s*v4\\.00\\+\\s*$"
            : "(?im)^ScriptType:\\s*v4\\.00\\s*$";
        return Pattern.compile("(?im)^\\[Script Info]\\s*$").matcher(normalized).find()
            && Pattern.compile(scriptType).matcher(normalized).find()
            && Pattern.compile("(?im)^\\[Events]\\s*$").matcher(normalized).find()
            && Pattern.compile("(?im)^Format:\\s*[^\\n]*Start[^\\n]*End[^\\n]*Text\\s*$")
                .matcher(normalized).find()
            && Pattern.compile("(?im)^Dialogue:\\s*[^,]*,\\d+:\\d{2}:\\d{2}\\.\\d{2},"
                + "\\d+:\\d{2}:\\d{2}\\.\\d{2},.*$").matcher(normalized).find();
    }

    private static boolean isVtt(String text) {
        String normalized = text.stripLeading();
        if (!normalized.startsWith("WEBVTT")) {
            return false;
        }
        int lineEnd = normalized.indexOf('\n');
        if (lineEnd < 0 || !normalized.substring(0, lineEnd).stripTrailing().equals("WEBVTT")) {
            return false;
        }
        return VTT_TIMELINE.matcher(normalized.substring(lineEnd + 1)).find();
    }

    private static boolean isIdx(String text) {
        return Pattern.compile("(?im)^#\\s*VobSub index file.*$").matcher(text).find()
            && IDX_TIMELINE.matcher(text).find();
    }

    private static boolean isTtml(byte[] data) {
        try {
            DocumentBuilderFactory factory = secureDocumentBuilderFactory();
            var builder = factory.newDocumentBuilder();
            builder.setEntityResolver((publicId, systemId) -> {
                throw new SAXException("external entities are disabled");
            });
            Document document = builder.parse(new ByteArrayInputStream(data));
            Element root = document.getDocumentElement();
            String namespace = root.getNamespaceURI();
            if (!"tt".equals(root.getLocalName())
                || !("http://www.w3.org/ns/ttml".equals(namespace)
                || "http://www.w3.org/2006/10/ttaf1".equals(namespace))) {
                return false;
            }
            NodeList bodies = document.getElementsByTagNameNS(namespace, "body");
            NodeList paragraphs = document.getElementsByTagNameNS(namespace, "p");
            if (bodies.getLength() == 0 || paragraphs.getLength() == 0) {
                return false;
            }
            for (int i = 0; i < paragraphs.getLength(); i++) {
                Element paragraph = (Element) paragraphs.item(i);
                if (!paragraph.getTextContent().isBlank()
                    && (paragraph.hasAttribute("begin")
                    || paragraph.hasAttribute("end") || paragraph.hasAttribute("dur"))) {
                    return true;
                }
            }
            return false;
        } catch (Exception exception) {
            return false;
        }
    }

    private static DocumentBuilderFactory secureDocumentBuilderFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setSchema(null);
        return factory;
    }

    private static Optional<String> decodeUtf8(byte[] data) {
        if (isPe(data) || isZip(data)) {
            return Optional.empty();
        }
        int offset = startsWith(data, hex("efbbbf")) ? 3 : 0;
        for (int i = offset; i < data.length; i++) {
            int value = unsigned(data[i]);
            if (value == 0 || value < 0x20 && value != '\t' && value != '\n' && value != '\r') {
                return Optional.empty();
            }
        }
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(data, offset, data.length - offset)).toString();
            return text.isBlank() ? Optional.empty() : Optional.of(text);
        } catch (CharacterCodingException exception) {
            return Optional.empty();
        }
    }

    private static boolean isPe(byte[] data) {
        if (data.length < 64 || data[0] != 'M' || data[1] != 'Z') {
            return false;
        }
        long peOffset = readUInt32Le(data, 0x3c);
        return peOffset <= data.length - 4L && asciiEquals(data, (int) peOffset, "PE\0\0");
    }

    private static boolean isZip(byte[] data) {
        return data.length >= 4 && data[0] == 'P' && data[1] == 'K'
            && ((data[2] == 3 && data[3] == 4) || (data[2] == 5 && data[3] == 6)
            || (data[2] == 7 && data[3] == 8));
    }

    private static VarInt readEbmlVarInt(byte[] data, int offset) {
        if (offset >= data.length || data[offset] == 0) {
            return null;
        }
        int mask = 0x80;
        int length = 1;
        while ((unsigned(data[offset]) & mask) == 0 && length <= 8) {
            mask >>= 1;
            length++;
        }
        if (length > 8 || offset + length > data.length) {
            return null;
        }
        long value = unsigned(data[offset]) & (mask - 1);
        for (int i = 1; i < length; i++) {
            value = value << 8 | unsigned(data[offset + i]);
        }
        long unknown = (1L << (7 * length)) - 1;
        return value == unknown ? null : new VarInt(length, value);
    }

    private static boolean containsBrand(String[] brands, String... expected) {
        return Arrays.stream(brands).anyMatch(brand -> Arrays.asList(expected).contains(brand));
    }

    private static boolean startsWith(byte[] data, byte[] expected) {
        return matches(data, 0, expected);
    }

    private static boolean matches(byte[] data, int offset, byte[] expected) {
        if (offset < 0 || offset + expected.length > data.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (data[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean asciiEquals(byte[] data, int offset, String expected) {
        return matches(data, offset, expected.getBytes(StandardCharsets.US_ASCII));
    }

    private static String ascii(byte[] data, int offset, int length) {
        return new String(data, offset, length, StandardCharsets.US_ASCII);
    }

    private static int unsigned(byte value) {
        return Byte.toUnsignedInt(value);
    }

    private static int readUInt16Be(byte[] data, int offset) {
        return unsigned(data[offset]) << 8 | unsigned(data[offset + 1]);
    }

    private static int readUInt16Le(byte[] data, int offset) {
        return unsigned(data[offset]) | unsigned(data[offset + 1]) << 8;
    }

    private static int readUInt24Be(byte[] data, int offset) {
        return unsigned(data[offset]) << 16 | unsigned(data[offset + 1]) << 8
            | unsigned(data[offset + 2]);
    }

    private static long readUInt32Be(byte[] data, int offset) {
        return Integer.toUnsignedLong(ByteBuffer.wrap(data, offset, 4).getInt());
    }

    private static long readUInt32Le(byte[] data, int offset) {
        return Integer.toUnsignedLong(ByteBuffer.wrap(data, offset, 4)
            .order(ByteOrder.LITTLE_ENDIAN).getInt());
    }

    private static long readUInt64Le(byte[] data, int offset) {
        long value = ByteBuffer.wrap(data, offset, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        return value < 0 ? Long.MAX_VALUE : value;
    }

    private static byte[] hex(String value) {
        return java.util.HexFormat.of().parseHex(value);
    }

    /** EBML 可变长整数的长度和值。 */
    private record VarInt(int length, long value) {
    }
}
