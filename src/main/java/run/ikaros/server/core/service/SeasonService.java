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
    SeasonDTO matchingEpisodesUrlByFileIds(@Nonnull SeasonMatchingEpParams seasonMatchingEpParams);

    void updateEpisodeUrlByFileEntity(@Nonnull FileEntity fileEntity);

    @Nullable
    List<SeasonEntity> findSeasonEntityByTitleLike(@Nonnull String title);

    @Nullable
    List<SeasonEntity> findSeasonEntityByTitleCnLike(@Nonnull String titleCn);

    @Nonnull
    SeasonDTO matchingEpisodeUrlByFileId(@Nonnull SeasonMatchingEpParams seasonMatchingEpParams);

    @Nonnull
    List<SeasonDTO> findDtoListByAnimeId(@Nonnull Long id);
}
