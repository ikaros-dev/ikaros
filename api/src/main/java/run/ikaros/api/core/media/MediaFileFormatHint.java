package run.ikaros.api.core.media;

import java.util.Set;

/**
 * 提供给客户端展示和文件选择器使用的媒体格式提示.
 *
 * @param format     真实格式
 * @param category   真实类别
 * @param mimeType   规范 MIME
 * @param extensions 允许扩展名
 */
public record MediaFileFormatHint(MediaFileFormat format,
                                  MediaFileCategory category,
                                  String mimeType,
                                  Set<String> extensions) {

    public MediaFileFormatHint {
        extensions = Set.copyOf(extensions);
    }

    public static MediaFileFormatHint from(MediaFileFormat format) {
        return new MediaFileFormatHint(format, format.category(), format.mimeType(),
            format.extensions());
    }
}
