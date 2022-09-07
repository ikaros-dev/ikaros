package cn.liguohao.ikaros.service;

import cn.liguohao.ikaros.common.SystemVarKit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * @author guohao
 * @date 2022/09/07
 */
@SpringBootTest
class FileServiceTest {

    @Resource
    FileService fileService;

    @Test
    void upload() throws IOException {
        // 在缓存目录生成一个文件
        final String currentAppDirPath = SystemVarKit.getCurrentAppDirPath();
        final String cacheDir = currentAppDirPath + "cache";
        File cacheFileDir = new File(cacheDir);
        if (!cacheFileDir.exists()) {
            cacheFileDir.mkdirs();
        }
        final String cacheFilePath = cacheDir + "test.txt";
        final String cacheFileContent = "Hello World";
        File cacheFile = new File(cacheFilePath);
        if (!cacheFile.exists()) {
            cacheFile.createNewFile();
        }
        try (FileOutputStream fileOutputStream = new FileOutputStream(cacheFile);) {
            fileOutputStream.write(cacheFileContent.getBytes(StandardCharsets.UTF_8));
            fileOutputStream.flush();
        }
        // 检查文件成功写入
        Assertions.assertTrue(cacheFile.length() > 0);

        // 调用服务层上传这个文件

        // 查询文件是否存在

        // 移除数据库的这个文件

        // 移除缓存目录对应的文件

    }

    @Test
    void download() {
    }

    @Test
    void update() {
    }

    @Test
    void testUpdate() {
    }

    @Test
    void delete() {
    }
}