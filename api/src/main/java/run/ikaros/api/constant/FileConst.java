package run.ikaros.api.constant;

/**
 * 文件存储目录相关常量。
 */
public interface FileConst {
    /** 工作目录中的默认文件存储目录名称。 */
    String DEFAULT_DIR_NAME = "files";

    /** 默认缓存目录名称。 */
    String DEFAULT_CACHE_DIR_NAME = "caches";

    /** 默认导入目录名称。 */
    String DEFAULT_IMPORT_DIR_NAME = "links";

    /** 默认根文件夹标识。 */
    Long DEFAULT_FOLDER_ROOT_ID = 0L;

    /** 默认根文件夹名称。 */
    String DEFAULT_FOLDER_ROOT_NAME = "root";

    /** 默认文件夹标识。 */
    Long DEFAULT_FOLDER_ID = DEFAULT_FOLDER_ROOT_ID;

    /** 默认文件夹名称。 */
    String DEFAULT_FOLDER_NAME = DEFAULT_DIR_NAME;

    /** 默认上传文件夹名称。 */
    String DEFAULT_UPLOAD_FOLDER_NAME = DEFAULT_FOLDER_NAME;
}
