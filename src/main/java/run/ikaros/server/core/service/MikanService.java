package run.ikaros.server.core.service;

import javax.annotation.Nonnull;
import org.springframework.retry.annotation.Retryable;

/**
 * @author li-guohao
 */
public interface MikanService {
    String BASE_URL = "https://mikanani.me";

    @Nonnull
    @Retryable
    String getAnimePageUrlByEpisodePageUrl(@Nonnull String episodePageUrl);

    @Nonnull
    @Retryable
    String getBgmTvSubjectPageUrlByAnimePageUrl(@Nonnull String animePageUrl);
}
