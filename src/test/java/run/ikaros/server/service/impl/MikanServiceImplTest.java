package run.ikaros.server.service.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import run.ikaros.server.core.service.MikanService;
import run.ikaros.server.service.MikanServiceImpl;

/**
 * @author li-guohao
 */
class MikanServiceImplTest {
    MikanService mikanService = new MikanServiceImpl();

    @Test
    void getAnimePageUrlByEpisodePageUrl() {
        String episodePageUrl =
            "https://mikanani.me/Home/Episode/8c7074d6603f2da3fa48cbb1ae6cc1a9056d4d2e";
        String animePageUrl = mikanService.getAnimePageUrlByEpisodePageUrl(episodePageUrl);
        Assertions.assertNotNull(animePageUrl);
    }

    @Test
    void getBgmTvSubjectPageUrlByAnimePageUrl() {
        String animePageUrl = "https://mikanani.me/Home/Bangumi/2830";
        String bgmTvSubjectPageUrl =
            mikanService.getBgmTvSubjectPageUrlByAnimePageUrl(animePageUrl);
        Assertions.assertNotNull(bgmTvSubjectPageUrl);
    }
}