package run.ikaros.server.service;

import java.util.List;
import javax.annotation.Nonnull;
import run.ikaros.server.constants.RegexConst;
import run.ikaros.server.entity.EpisodeEntity;
import run.ikaros.server.service.base.CrudService;
import run.ikaros.server.utils.AssertUtils;

/**
 * @author li-guohao
 */
public interface EpisodeService extends CrudService<EpisodeEntity, Long> {
    @Nonnull
    List<EpisodeEntity> findBySeasonId(@Nonnull Long seasonId);

}
