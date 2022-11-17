package run.ikaros.server.core.service;

import javax.annotation.Nonnull;
import javax.transaction.Transactional;
import run.ikaros.server.entity.AnimeEntity;
import run.ikaros.server.model.dto.AnimeDTO;
import run.ikaros.server.params.SearchAnimeDTOSParams;
import run.ikaros.server.result.PagingWrap;

/**
 * @author li-guohao
 */
public interface AnimeService extends CrudService<AnimeEntity, Long> {

    @Nonnull
    @Transactional(rollbackOn = Exception.class)
    AnimeDTO saveAnimeDTO(@Nonnull AnimeDTO animeDTO);

    @Nonnull
    AnimeDTO findAnimeDTOById(@Nonnull Long id);

    @Nonnull
    PagingWrap<AnimeDTO> findAnimeDTOS(@Nonnull SearchAnimeDTOSParams searchAnimeDTOSParams);
}
