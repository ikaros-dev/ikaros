package run.ikaros.server.service;

import java.util.List;
import javax.annotation.Nonnull;
import run.ikaros.server.entity.SeasonEntity;
import run.ikaros.server.service.base.CrudService;

/**
 * @author li-guohao
 */
public interface SeasonService extends CrudService<SeasonEntity, Long> {

    @Nonnull
    List<SeasonEntity> findByAnimeId(@Nonnull Long animeId);

    List<String> finSeasonTypes();
}
