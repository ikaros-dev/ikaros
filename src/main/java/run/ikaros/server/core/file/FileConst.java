package run.ikaros.server.core.file;

public interface FileConst {
    String POLICY_LOCAL = "LOCAL";
    String LOCAL_UPLOAD_DIR_NAME = "upload";
    Long DEFAULT_FOLDER_ID = -1L;


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
