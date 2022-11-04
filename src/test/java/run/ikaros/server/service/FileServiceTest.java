package run.ikaros.server.service;

import java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import run.ikaros.server.common.UnitTestConst;
import run.ikaros.server.entity.FileEntity;
import run.ikaros.server.utils.JsonUtils;
import run.ikaros.server.utils.SystemVarUtils;
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
        final String currentAppDirPath = SystemVarUtils.getCurrentAppDirPath();
        final String cacheDir = SystemVarUtils.getOsCacheDirPath() + File.separator + "cache";
        File cacheFileDir = new File(cacheDir);
        if (!cacheFileDir.exists()) {
            cacheFileDir.mkdirs();
        }
        final String cacheFilePath = cacheDir + File.separator + "test.txt";
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
        FileEntity fileEntity =
            fileService.upload(cacheFile.getName(), Files.readAllBytes(cacheFile.toPath()));

        // 查询文件是否存在
        Assertions.assertEquals(cacheFile.getName(), fileEntity.getName());
        Assertions.assertEquals(cacheFile.length(), (long) fileEntity.getSize());
        Assertions.assertNotNull(fileEntity.getUrl());
        String url = fileEntity.getUrl();
        String relativePath = url.replace("/", File.separator);
        File exceptFile = new File(currentAppDirPath + File.separator + relativePath);
        Assertions.assertTrue(exceptFile.length() > 0);

        // 移除数据库的这个文件
        fileService.delete(fileEntity.getId());
        // 移除这个上传的文件
        if(!Files.deleteIfExists(exceptFile.toPath())) {
            Assertions.fail(UnitTestConst.PROCESS_SHOUT_NOT_RUN_THIS);
        }

        // 移除缓存目录对应的文件
        if(! Files.deleteIfExists(cacheFile.toPath())) {
            Assertions.fail(UnitTestConst.PROCESS_SHOUT_NOT_RUN_THIS);
        }

    }

    @Test
    void getEpisodeSeqFromName() {
        String tagSeqFileName = "[VCB-Studio] K-ON! [01][Ma10p_1080p][x265_flac_2aac].mkv";
        String numSeqFileName = "Cyberpunk Edgerunners 11.mp4";

        Long tagSeq = fileService.getEpisodeSeqFromName(tagSeqFileName);
        Assertions.assertNotNull(tagSeq);
        Assertions.assertEquals(01L, tagSeq);

        Long numSeq = fileService.getEpisodeSeqFromName(numSeqFileName);
        Assertions.assertNotNull(numSeq);
        Assertions.assertEquals(11L, numSeq);

        Long seq = fileService.getEpisodeSeqFromName(
            "[Lilith-Raws] SPYxFAMILY - 15 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4].mp4");
        Assertions.assertEquals(15L, seq);
        seq = fileService.getEpisodeSeqFromName(
            "[Lilith-Raws] SPYxFAMILY - 17 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4].mp4");
        Assertions.assertEquals(17L, seq);


    }
}