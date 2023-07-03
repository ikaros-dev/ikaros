package run.ikaros.api.constant;

public interface FileConst {
    /**
     * File import(upload) dir name in work dir.
     */
    String DEFAULT_DIR_NAME = "files";
    Long DEFAULT_FOLDER_ROOT_ID = 0L;
    String DEFAULT_FOLDER_ROOT_NAME = "root";
    Long DEFAULT_FOLDER_ID = DEFAULT_FOLDER_ROOT_ID;
    String DEFAULT_FOLDER_NAME = DEFAULT_FOLDER_ROOT_NAME;

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
            "txt", "doc", "docx", "ppt", "xlsx", "pptx", "ass"
        };
        String[] VOICES = {
            "mp3", "wma", "wav", "ape", "flac", "ogg", "aac"
        };
    }
}
