package run.ikaros.server.service.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import run.ikaros.server.core.service.MikanService;
import run.ikaros.server.core.service.OptionService;
import run.ikaros.server.service.MikanServiceImpl;
import run.ikaros.server.service.OptionServiceImpl;

/**
 * @author li-guohao
 */
class MikanServiceImplTest {
    OptionService optionService = Mockito.mock(OptionServiceImpl.class);
    MikanService mikanService = new MikanServiceImpl(optionService);

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