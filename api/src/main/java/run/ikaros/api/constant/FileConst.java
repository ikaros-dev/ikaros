package run.ikaros.api.constant;

/**
 * 文件存储目录相关常量.
 */
public interface FileConst {
    /** 工作目录中的默认文件存储目录名称. */
    String DEFAULT_DIR_NAME = "files";

    /** 默认缓存目录名称. */
    String DEFAULT_CACHE_DIR_NAME = "caches";

    /** 默认导入目录名称. */
    String DEFAULT_IMPORT_DIR_NAME = "links";

    /** 默认根文件夹标识. */
    Long DEFAULT_FOLDER_ROOT_ID = 0L;

    /** 默认根文件夹名称. */
    String DEFAULT_FOLDER_ROOT_NAME = "root";

    /** 默认文件夹标识. */
    Long DEFAULT_FOLDER_ID = DEFAULT_FOLDER_ROOT_ID;

    /** 默认文件夹名称. */
    String DEFAULT_FOLDER_NAME = DEFAULT_DIR_NAME;

    /** 默认上传文件夹名称. */
    String DEFAULT_UPLOAD_FOLDER_NAME = DEFAULT_FOLDER_NAME;

    /**
     * 常见媒体文件后缀常量.
     *
     * <p>为保持 API 二进制兼容性而保留，已不再被内部代码使用，
     * 建议改用 {@code run.ikaros.api.core.media.MediaFilePolicy} 与
     * {@code MediaFileFormat}/{@code MediaFileCategory} 判定媒体类别。
     *
     * @deprecated 由媒体格式策略取代，仅用于向后兼容。
     */
    @Deprecated
    interface Postfix {
        String[] IMAGES = {
            "jpg", "jpeg", "png", "gif", "webp"
        };

        String[] VIDEOS = {
            "mkv", "mp4", "avi", "flv", "f4v", "webm", "m4v", "mov", "3gp", "3g2", "rm", "rmvb",
            "wmv", "asf", "mpg", "mpeg", "mpe", "ts", "div", "dv", "divx", "vob", "dat", "lavf",
            "cpk", "dirac", "ram", "qt", "fli", "flc", "mod", "mpg", "mlv", "mpe", "mpeg", "m3u8"
        };

        String[] DOCUMENTS = {
            "txt", "doc", "docx", "ppt", "xlsx", "pptx", "ass", "md", "mdc", "log",
            "html", "htm", "shtml", "xhtml",
            "css", "js", "mjs", "xml", "rtf", "csv"
        };
        String[] VOICES = {
            "mp3", "wma", "wav", "ape", "flac", "ogg", "aac"
        };
    }
}
