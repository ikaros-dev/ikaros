package run.ikaros.server.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import run.ikaros.server.core.service.EpisodeService;
import run.ikaros.server.core.service.FileService;
import run.ikaros.server.core.service.SeasonService;
import run.ikaros.server.entity.EpisodeEntity;
import run.ikaros.server.entity.FileEntity;
import run.ikaros.server.entity.SeasonEntity;
import run.ikaros.server.enums.FileType;
import run.ikaros.server.enums.SeasonType;
import run.ikaros.server.model.dto.EpisodeDTO;
import run.ikaros.server.model.dto.SeasonDTO;
import run.ikaros.server.params.SeasonMatchingEpParams;
import run.ikaros.server.utils.StringUtils;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author li-guohao
 */
@SpringBootTest
class SeasonServiceTest {

    @Resource
    SeasonService seasonService;
    @Resource
    EpisodeService episodeService;
    @Resource
    FileService fileService;

    @Test
    void matchingEpisodeUrlByFileIds() {
        // 准备数据
        SeasonEntity seasonEntity = new SeasonEntity()
            .setType(SeasonType.FIRST)
            .setAnimeId(-1L)
            .setTitle("TEST_SEASON_FIRST");
        seasonEntity = seasonService.save(seasonEntity);

        List<Long> fileIdList = new ArrayList<>();
        List<Long> episodeIdList = new ArrayList<>();

        final int count = 24;

        for (int i = 0; i < count; i++) {
            EpisodeEntity episodeEntity = episodeService.save(new EpisodeEntity()
                .setSeasonId(seasonEntity.getId())
                .setTitle("EP01" + i)
                .setSeq((long) (i + 1))
                .setAirTime(new Date())
            );
            episodeIdList.add(episodeEntity.getId());

            FileEntity fileEntity = new FileEntity();
            fileEntity.setMd5("MD5");
            fileEntity.setType(FileType.VIDEO);
            fileEntity.setUrl((i + 1) + "");
            fileEntity.setSize(-1);
            fileEntity.setName("[VCB-Studio] Sora no Otoshimono II [" + (i + 1)
                + "][Hi10p_1080p][x264_2flac].mkv");
            fileEntity = fileService.save(fileEntity);
            fileIdList.add(fileEntity.getId());
        }

        Optional<EpisodeEntity> episodeEntityOptional =
            episodeService.fetchById(episodeIdList.get(0));
        Assertions.assertTrue(episodeEntityOptional.isPresent());
        Assertions.assertTrue(StringUtils.isBlank(episodeEntityOptional.get().getUrl()));

        // 调用方法
        SeasonMatchingEpParams seasonMatchingEpParams = new SeasonMatchingEpParams();
        seasonMatchingEpParams.setSeasonId(seasonEntity.getId());
        seasonMatchingEpParams.setFileIdList(fileIdList);
        SeasonDTO seasonDTO = seasonService.matchingEpisodesUrlByFileIds(seasonMatchingEpParams);

        Assertions.assertNotNull(seasonDTO);
        List<EpisodeDTO> episodes = seasonDTO.getEpisodes();
        Assertions.assertNotNull(episodes);
        Assertions.assertFalse(episodes.isEmpty());
        Assertions.assertEquals(count, episodes.size());

        episodes.forEach(episodeDTO -> {
            String url = episodeDTO.getUrl();
            Long urlLong = Long.parseLong(url);
            Long seq = episodeDTO.getSeq();
            Assertions.assertEquals(seq, urlLong);
        });

        // 清空数据
        episodeService.deleteAll();
        seasonService.deleteAll();
    }
}