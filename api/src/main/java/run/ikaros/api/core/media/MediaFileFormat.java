package run.ikaros.api.core.media;

import java.util.Set;

/**
 * 媒体文件真实格式的权威定义。
 */
public enum MediaFileFormat {
    PNG(MediaFileCategory.IMAGE, "image/png", "png"),
    JPEG(MediaFileCategory.IMAGE, "image/jpeg", "jpg", "jpeg"),
    GIF(MediaFileCategory.IMAGE, "image/gif", "gif"),
    WEBP(MediaFileCategory.IMAGE, "image/webp", "webp"),
    BMP(MediaFileCategory.IMAGE, "image/bmp", "bmp"),
    AVIF(MediaFileCategory.IMAGE, "image/avif", "avif"),

    MP4(MediaFileCategory.VIDEO, "video/mp4", "mp4", "m4v"),
    QUICKTIME(MediaFileCategory.VIDEO, "video/quicktime", "mov"),
    MATROSKA(MediaFileCategory.VIDEO, "video/x-matroska", "mkv"),
    WEBM(MediaFileCategory.VIDEO, "video/webm", "webm"),
    AVI(MediaFileCategory.VIDEO, "video/x-msvideo", "avi"),
    FLV(MediaFileCategory.VIDEO, "video/x-flv", "flv"),
    MPEG_TS(MediaFileCategory.VIDEO, "video/mp2t", "ts", "m2ts"),
    WMV(MediaFileCategory.VIDEO, "video/x-ms-wmv", "wmv"),

    MP3(MediaFileCategory.AUDIO, "audio/mpeg", "mp3"),
    AAC(MediaFileCategory.AUDIO, "audio/aac", "aac"),
    M4A(MediaFileCategory.AUDIO, "audio/mp4", "m4a"),
    FLAC(MediaFileCategory.AUDIO, "audio/flac", "flac"),
    OGG(MediaFileCategory.AUDIO, "audio/ogg", "ogg"),
    OPUS(MediaFileCategory.AUDIO, "audio/opus", "opus"),
    WAV(MediaFileCategory.AUDIO, "audio/wav", "wav"),
    WMA(MediaFileCategory.AUDIO, "audio/x-ms-wma", "wma"),

    SRT(MediaFileCategory.SUBTITLE, "application/x-subrip", "srt"),
    ASS(MediaFileCategory.SUBTITLE, "text/x-ssa", "ass"),
    SSA(MediaFileCategory.SUBTITLE, "text/x-ssa", "ssa"),
    VTT(MediaFileCategory.SUBTITLE, "text/vtt", "vtt"),
    MICRODVD(MediaFileCategory.SUBTITLE, "text/plain", "sub"),
    VOBSUB(MediaFileCategory.SUBTITLE, "application/octet-stream", "sub"),
    IDX(MediaFileCategory.SUBTITLE, "text/plain", "idx"),
    TTML(MediaFileCategory.SUBTITLE, "application/ttml+xml", "ttml"),
    LRC(MediaFileCategory.LYRICS, "text/plain", "lrc");

    /** 媒体文件的真实类别。 */
    private final MediaFileCategory category;

    /** 媒体文件的规范 MIME。 */
    private final String mimeType;

    /** 允许进入真实格式检测的扩展名。 */
    private final Set<String> extensions;

    MediaFileFormat(MediaFileCategory category, String mimeType, String... extensions) {
        this.category = category;
        this.mimeType = mimeType;
        this.extensions = Set.of(extensions);
    }

    public MediaFileCategory category() {
        return category;
    }

    public String mimeType() {
        return mimeType;
    }

    public Set<String> extensions() {
        return extensions;
    }
}
