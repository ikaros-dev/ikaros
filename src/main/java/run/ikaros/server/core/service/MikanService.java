package run.ikaros.server.core.service;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.RestTemplate;

/**
 * @author li-guohao
 */
public interface MikanService {
    String BASE_URL = "https://mikanani.me";

    void setRestTemplate(@Nonnull RestTemplate restTemplate);

    @Nonnull
    @Retryable
    String getAnimePageUrlByEpisodePageUrl(@Nonnull String episodePageUrl);

    @Nonnull
    @Retryable
    String getBgmTvSubjectPageUrlByAnimePageUrl(@Nonnull String animePageUrl);

    @Nullable
    @Retryable
    String getAnimePageUrlBySearch(@Nonnull String keyword);
}
