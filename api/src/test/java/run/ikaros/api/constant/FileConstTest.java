package run.ikaros.api.constant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 文件目录常量测试。
 */
class FileConstTest {

    @Test
    void shouldKeepDirectoryConstants() {
        assertThat(FileConst.DEFAULT_DIR_NAME).isEqualTo("files");
        assertThat(FileConst.DEFAULT_CACHE_DIR_NAME).isEqualTo("caches");
        assertThat(FileConst.DEFAULT_IMPORT_DIR_NAME).isEqualTo("links");
        assertThat(FileConst.DEFAULT_FOLDER_ROOT_ID).isZero();
        assertThat(FileConst.DEFAULT_FOLDER_ROOT_NAME).isEqualTo("root");
        assertThat(FileConst.DEFAULT_FOLDER_ID).isEqualTo(FileConst.DEFAULT_FOLDER_ROOT_ID);
        assertThat(FileConst.DEFAULT_FOLDER_NAME).isEqualTo(FileConst.DEFAULT_DIR_NAME);
        assertThat(FileConst.DEFAULT_UPLOAD_FOLDER_NAME).isEqualTo(FileConst.DEFAULT_FOLDER_NAME);
    }
}
