package run.ikaros.server.service;


import run.ikaros.server.model.bgmtv.BgmTvEpisode;
import run.ikaros.server.model.bgmtv.BgmTvEpisodeType;
import run.ikaros.server.model.bgmtv.BgmTvSubject;
import run.ikaros.server.entity.FileEntity;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import run.ikaros.server.service.BgmTvService;

/**
 * @author guohao
 * @date 2022/10/20
 */
@SpringBootTest
class BgmTvServiceTest {

    @Autowired
    BgmTvService bgmTvService;

    Long subjectId = 373267L;

    @Test
    void getSubjectMetadata() {
        BgmTvSubject bgmTvSubject = bgmTvService.getSubject(subjectId);
        Assertions.assertNotNull(bgmTvSubject);
        Assertions.assertNotNull(bgmTvSubject.getName());
    }

    @Test
    void getEpisodesBySubjectId() {
        List<BgmTvEpisode> bgmTvEpisodes =
            bgmTvService.getEpisodesBySubjectId(subjectId, BgmTvEpisodeType.POSITIVE);
        Assertions.assertNotNull(bgmTvEpisodes);
        Assertions.assertFalse(bgmTvEpisodes.isEmpty());
    }

    @Test
    void downloadCover() throws IOException {
        String url = "https://lain.bgm.tv/pic/cover/l/3c/82/373267_ffBO8.jpg";
        FileEntity fileEntity = bgmTvService.downloadCover(url);
        Assertions.assertNotNull(fileEntity);
        Assertions.assertNotNull(fileEntity.getLocation());
        Assertions.assertNotNull(fileEntity.getUrl());
        Files.deleteIfExists(Path.of(new File(fileEntity.getLocation()).toURI()));
    }
}