package run.ikaros.server.service;

import run.ikaros.server.entity.FileEntity;
import run.ikaros.server.entity.SeasonEntity;
import run.ikaros.server.model.dto.SeasonDTO;
import run.ikaros.server.params.SeasonMatchingEpParams;
import run.ikaros.server.service.base.CrudService;

import javax.annotation.Nonnull;
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
}
