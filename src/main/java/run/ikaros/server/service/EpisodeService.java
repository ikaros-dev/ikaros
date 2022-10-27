package run.ikaros.server.service;

import java.util.List;
import javax.annotation.Nonnull;
import run.ikaros.server.entity.EpisodeEntity;
import run.ikaros.server.service.base.CrudService;

/**
 * @author li-guohao
 */
public interface EpisodeService extends CrudService<EpisodeEntity, Long> {
    @Nonnull
    List<EpisodeEntity> findBySeasonId(@Nonnull Long seasonId);

}
