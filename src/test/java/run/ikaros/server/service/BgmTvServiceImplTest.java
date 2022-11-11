package run.ikaros.server.service;


import org.junit.jupiter.api.Disabled;
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
import run.ikaros.server.service.impl.BgmTvServiceImpl;

/**
 * @author guohao
 * @date 2022/10/20
 */
@SpringBootTest
class BgmTvServiceImplTest {

    @Autowired
    BgmTvServiceImpl bgmTvServiceImpl;

    Long subjectId = 373267L;

    @Test
    void getSubjectMetadata() {
        BgmTvSubject bgmTvSubject = bgmTvServiceImpl.getSubject(subjectId);
        Assertions.assertNotNull(bgmTvSubject);
        Assertions.assertNotNull(bgmTvSubject.getName());
    }

    @Test
    void getEpisodesBySubjectId() {
        List<BgmTvEpisode> bgmTvEpisodes =
            bgmTvServiceImpl.getEpisodesBySubjectId(subjectId, BgmTvEpisodeType.POSITIVE);
        Assertions.assertNotNull(bgmTvEpisodes);
        Assertions.assertFalse(bgmTvEpisodes.isEmpty());
    }

    @Test
    void downloadCover() throws IOException {
        String url = "https://lain.bgm.tv/pic/cover/l/3c/82/373267_ffBO8.jpg";
        FileEntity fileEntity = bgmTvServiceImpl.downloadCover(url);
        Assertions.assertNotNull(fileEntity);
        Assertions.assertNotNull(fileEntity.getUrl());
        Assertions.assertNotNull(fileEntity.getUrl());
        Files.deleteIfExists(Path.of(new File(fileEntity.getUrl()).toURI()));
    }

    @Test
    @Disabled("行为不符合预期")
    void findSubjectByQueryStr() {
        BgmTvSubject bgmTvSubject = bgmTvServiceImpl.findSubjectByQueryStr("Do It Yourself");
        Assertions.assertNotNull(bgmTvSubject);
        Assertions.assertNotNull(bgmTvSubject.getId());
    }


}