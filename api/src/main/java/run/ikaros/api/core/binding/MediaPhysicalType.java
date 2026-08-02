package run.ikaros.api.core.binding;

/** 根据文件最终扩展名识别的物理媒体类型。 */
public enum MediaPhysicalType {
    /** 视频文件。 */
    VIDEO,
    /** 音频文件。 */
    AUDIO,
    /** 字幕文件。 */
    SUBTITLE,
    /** 歌词文件。 */
    LYRICS,
    /** 图片文件。 */
    IMAGE,
    /** 未识别的文件。 */
    UNKNOWN
}
