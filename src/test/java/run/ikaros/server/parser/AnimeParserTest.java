package run.ikaros.server.parser;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import run.ikaros.server.utils.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AnimeParserTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnimeParserTest.class);

    @Test
    void parseInfoByEpisodeFileName() {

        String fileName = "[VCB-Studio] K-ON!! [01][Ma10p_1080p][x265_flac_2aac].mkv";
        AnimeEpisodeInfo animeEpisodeInfo = AnimeParser.parseInfoByEpisodeFileName(fileName);
        LOGGER.info("anime episode info: {}", JsonUtils.obj2Json(animeEpisodeInfo));
        assertThat(animeEpisodeInfo).isNotNull();
    }
}