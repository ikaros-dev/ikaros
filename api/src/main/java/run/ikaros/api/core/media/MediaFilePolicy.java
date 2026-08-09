package run.ikaros.api.core.media;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 媒体文件名门禁、格式查询和最终检测结果校验策略。
 */
public final class MediaFilePolicy {

    private MediaFilePolicy() {
    }

    public static Optional<String> extractExtension(String filename) {
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

    public static boolean isAllowedFileName(String filename) {
        return extractExtension(filename).isPresent();
    }

    public static List<MediaFileFormat> formatsForExtension(String extension) {
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

    public static List<MediaFileFormatHint> hintsForExtension(String extension) {
        return formatsForExtension(extension)
            .stream()
            .map(MediaFileFormatHint::from)
            .toList();
    }

    public static List<MediaFileFormatHint> formatHints() {
        return Arrays
            .stream(MediaFileFormat.values())
            .map(MediaFileFormatHint::from)
            .toList();
    }

    public static boolean extensionHasCategory(String extension, MediaFileCategory category) {
        return formatsForExtension(extension)
            .stream()
            .anyMatch(format -> format.category() == category);
    }

    public static boolean isDetectionAllowed(String extension, MediaFileDetectionResult result) {
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

    private static String normalizeExtension(String extension) {
        if (extension == null || extension.isBlank()
            || extension.indexOf('.') >= 0 || extension.indexOf('/') >= 0
            || extension.indexOf('\\') >= 0) {
            return null;
        }
        return extension.toLowerCase(Locale.ROOT);
    }
}
