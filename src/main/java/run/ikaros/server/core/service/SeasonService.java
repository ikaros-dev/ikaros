package run.ikaros.server.core.service;

import run.ikaros.server.entity.FileEntity;
import run.ikaros.server.entity.SeasonEntity;
import run.ikaros.server.model.dto.SeasonDTO;
import run.ikaros.server.params.SeasonMatchingEpParams;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author li-guohao
 */
public interface SeasonService extends CrudService<SeasonEntity, Long> {

    @Nonnull
    List<SeasonEntity> findByAnimeId(@Nonnull Long animeId);

    @Nonnull
    List<String> finSeasonTypes();

    @Nonnull
    SeasonDTO matchingEpisodeUrlByFileIds(@Nonnull SeasonMatchingEpParams seasonMatchingEpParams);

    void updateEpisodeUrlByFileEntity(@Nonnull FileEntity fileEntity);

    @Nullable
    SeasonEntity findSeasonEntityByTitleLike(@Nonnull String title);

    @Nullable
    SeasonEntity findSeasonEntityByTitleCnLike(@Nonnull String titleCn);
}
