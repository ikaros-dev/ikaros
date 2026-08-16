package run.ikaros.api.core.media;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * 媒体文件名门禁、格式查询和最终检测结果校验策略.
 */
public final class MediaFilePolicy {

    private MediaFilePolicy() {
    }

    /**
     * 从文件名提取已知扩展名.
     *
     * @param filename 文件名
     * @return 已知扩展名，不符合要求时返回空
     */
    public static Optional<String> extractExtension(@Nullable String filename) {
        if (filename == null || filename.isBlank()
            || filename.indexOf('/') >= 0 || filename.indexOf('\\') >= 0) {
            return Optional.empty();
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == filename.length() - 1) {
            return Optional.empty();
        }
        String extension = filename
            .substring(dotIndex + 1)
            .toLowerCase(Locale.ROOT);
        return isKnownExtension(extension) ? Optional.of(extension) : Optional.empty();
    }

    public static boolean isAllowedFileName(@Nullable String filename) {
        return extractExtension(filename).isPresent();
    }

    /**
     * 查询扩展名对应的全部媒体格式.
     *
     * @param extension 文件扩展名
     * @return 对应的媒体格式列表
     */
    public static List<MediaFileFormat> formatsForExtension(@Nullable String extension) {
        String normalized = normalizeExtension(extension);
        if (normalized == null) {
            return List.of();
        }
        return Arrays
            .stream(MediaFileFormat.values())
            .filter(format -> format
                .extensions()
                .contains(normalized))
            .toList();
    }

    /**
     * 查询扩展名对应的全部媒体格式提示.
     *
     * @param extension 文件扩展名
     * @return 对应的媒体格式提示列表
     */
    public static List<MediaFileFormatHint> hintsForExtension(@Nullable String extension) {
        return formatsForExtension(extension)
            .stream()
            .map(MediaFileFormatHint::from)
            .toList();
    }

    /**
     * 获取全部媒体格式提示.
     *
     * @return 全部媒体格式提示
     */
    public static List<MediaFileFormatHint> formatHints() {
        return Arrays
            .stream(MediaFileFormat.values())
            .map(MediaFileFormatHint::from)
            .toList();
    }

    /**
     * 判断扩展名是否支持指定媒体类别.
     *
     * @param extension 文件扩展名
     * @param category 媒体类别
     * @return 支持指定类别时返回 true
     */
    public static boolean extensionHasCategory(@Nullable String extension,
                                               MediaFileCategory category) {
        return formatsForExtension(extension)
            .stream()
            .anyMatch(format -> format.category() == category);
    }

    /**
     * 判断检测结果是否符合扩展名允许的媒体格式.
     *
     * @param extension 文件扩展名
     * @param result 媒体检测结果
     * @return 检测结果符合扩展名约束时返回 true
     */
    public static boolean isDetectionAllowed(@Nullable String extension,
                                             @Nullable MediaFileDetectionResult result) {
        String normalized = normalizeExtension(extension);
        if (normalized == null || result == null || !isKnownExtension(normalized)) {
            return false;
        }
        MediaFileFormat format = result.format();
        if ("sub".equals(normalized)) {
            return format == MediaFileFormat.MICRODVD || format == MediaFileFormat.VOBSUB;
        }
        boolean strictExtension = formatsForExtension(normalized)
            .stream()
            .anyMatch(candidate -> candidate.category() == MediaFileCategory.SUBTITLE
                || candidate.category() == MediaFileCategory.LYRICS);
        if (strictExtension) {
            return format
                .extensions()
                .contains(normalized);
        }
        return format.category() == MediaFileCategory.IMAGE
            || format.category() == MediaFileCategory.AUDIO
            || format.category() == MediaFileCategory.VIDEO;
    }

    private static boolean isKnownExtension(String extension) {
        return Arrays
            .stream(MediaFileFormat.values())
            .anyMatch(format -> format
                .extensions()
                .contains(extension));
    }

    private static @Nullable String normalizeExtension(@Nullable String extension) {
        if (extension == null || extension.isBlank()
            || extension.indexOf('.') >= 0 || extension.indexOf('/') >= 0
            || extension.indexOf('\\') >= 0) {
            return null;
        }
        return extension.toLowerCase(Locale.ROOT);
    }
}
