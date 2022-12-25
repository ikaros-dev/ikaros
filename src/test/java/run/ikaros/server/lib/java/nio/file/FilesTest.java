package run.ikaros.server.lib.java.nio.file;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FilesTest {

    // @Test
    void moveFile() throws IOException {
        Files.move(new File(
                "C:\\Develop\\Temp\\exists\\开源媒体管理项目发布0.1.0公共测试版了.mp4")
                .toPath(),
            new File("C:\\Develop\\Temp\\target\\开源媒体管理项目发布0.1.0公共测试版了.mp4")
                .toPath());
    }

    // @Test
    void moveDir() throws IOException {
        Files.move(new File(
                "C:\\Develop\\Temp\\exists\\media")
                .toPath(),
            new File("C:\\Develop\\Temp\\target\\media")
                .toPath());
    }

}
