package run.ikaros.api.core.media;

import java.util.Objects;

/**
 * 媒体文件前缀检测确认的真实格式信息。
 *
 * @param format 真实格式
 * @param category 真实类别
 * @param mimeType 规范 MIME
 */
public record MediaFileDetectionResult(MediaFileFormat format,
                                       MediaFileCategory category,
                                       String mimeType) {

    public MediaFileDetectionResult {
        Objects.requireNonNull(format, "format must not be null");
        if (category != format.category() || !format.mimeType().equals(mimeType)) {
            throw new IllegalArgumentException("category and mimeType must match format");
        }
    }

    public MediaFileDetectionResult(MediaFileFormat format) {
        this(format, format.category(), format.mimeType());
    }
}
