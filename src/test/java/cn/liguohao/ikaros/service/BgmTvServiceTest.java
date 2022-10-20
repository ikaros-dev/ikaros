package cn.liguohao.ikaros.service;


import cn.liguohao.ikaros.model.bgmtv.Episode;
import cn.liguohao.ikaros.model.bgmtv.EpisodeType;
import cn.liguohao.ikaros.model.bgmtv.Subject;
import cn.liguohao.ikaros.model.entity.FileEntity;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
        Subject subject = bgmTvService.getSubject(subjectId);
        Assertions.assertNotNull(subject);
        Assertions.assertNotNull(subject.getName());
    }

    @Test
    void getEpisodesBySubjectId() {
        List<Episode> episodes =
            bgmTvService.getEpisodesBySubjectId(subjectId, EpisodeType.POSITIVE);
        Assertions.assertNotNull(episodes);
        Assertions.assertFalse(episodes.isEmpty());
    }

    @Test
    void downloadCover() {
        String url = "https://lain.bgm.tv/pic/cover/l/3c/82/373267_ffBO8.jpg";
        FileEntity fileEntity = bgmTvService.downloadCover(url);
        Assertions.assertNotNull(fileEntity);
        Assertions.assertNotNull(fileEntity.getLocation());
        Assertions.assertNotNull(fileEntity.getUrl());
    }
}