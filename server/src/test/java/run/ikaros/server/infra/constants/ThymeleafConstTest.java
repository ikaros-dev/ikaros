package run.ikaros.server.infra.constants;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ThymeleafConstTest {

    @Test
    void animeUpdateShouldNotBeNull() {
        assertThat(ThymeleafConst.ANIME_UPDATE).isNotNull();
    }

    @Test
    void animeUpdateShouldHaveCorrectValue() {
        assertThat(ThymeleafConst.ANIME_UPDATE).isEqualTo("mail/anime_update");
    }

    @Test
    void animeUpdateShouldNotBeEmpty() {
        assertThat(ThymeleafConst.ANIME_UPDATE).isNotEmpty();
    }

    @Test
    void animeUpdateShouldStartWithMailPrefix() {
        assertThat(ThymeleafConst.ANIME_UPDATE).startsWith("mail/");
    }
}
