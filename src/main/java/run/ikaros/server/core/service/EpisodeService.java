package run.ikaros.server.core.service;

import java.util.List;
import jakarta.annotation.Nonnull;

import run.ikaros.server.entity.EpisodeEntity;
import run.ikaros.server.model.dto.EpisodeDTO;

/**
 * @author li-guohao
 */
public interface EpisodeService extends CrudService<EpisodeEntity, Long> {
    @Nonnull
    List<EpisodeEntity> findBySeasonId(@Nonnull Long seasonId);

    List<EpisodeEntity> findBySeasonIdAndSeq(@Nonnull Long seasonId, @Nonnull Long seq);

    @Nonnull
    List<EpisodeDTO> findDtoListBySeasonId(@Nonnull Long id);
}
