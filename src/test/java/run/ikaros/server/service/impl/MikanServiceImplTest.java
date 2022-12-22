package run.ikaros.server.service.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;
import run.ikaros.server.core.service.MikanService;
import run.ikaros.server.core.service.OptionService;
import run.ikaros.server.tripartite.mikan.service.MikanServiceImpl;
import run.ikaros.server.service.OptionServiceImpl;
import run.ikaros.server.utils.RestTemplateUtils;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void getAnimePageUrlBySearch() {
        String keyword = "[LoliHouse] Shinmai Renkinjutsushi no Tenpo Keiei"
            + " - 12 [WebRip 1080p HEVC-10bit AAC SRTx2]";
        RestTemplate restTemplate =
            RestTemplateUtils.buildHttpProxyRestTemplate("127.0.0.1", 7890);
        mikanService.setRestTemplate(restTemplate);

        String animePageUrlBySearch = mikanService.getAnimePageUrlBySearch(keyword);
        assertThat(animePageUrlBySearch).isNotBlank();
    }
}