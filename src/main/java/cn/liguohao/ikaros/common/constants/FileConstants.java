package cn.liguohao.ikaros.common.constants;

/**
 * @author guohao
 * @date 2022/10/18
 */
public interface FileConstants {

    interface IkarosFile {
        String DEFAULT_POSTFIX = ".ikaros";
    }

    interface Postfix {
        String[] IMAGES = {
            "jpg", "jpeg", "png", "gif", "webp"
        };

        String[] VIDEOS = {
            "mkv", "mp4", "avi", "flv", "f4v", "webm", "m4v", "mov", "3gp", "3g2", "rm", "rmvb", "wmv",
            "asf", "mpg", "mpeg", "mpe", "ts", "div", "dv", "divx", "vob", "dat", "lavf", "cpk",
            "dirac", "ram", "qt", "fli", "flc", "mod", "mpg", "mlv", "mpe", "mpeg", "m3u8"
        };

        String[] DOCUMENTS = {
            "txt", "doc", "docx", "ppt", "xlsx", "pptx"
        };
        String[] VOICES = {
            "mp3", "wma", "wav", "ape", "flac", "ogg", "aac"
        };
    }

}
