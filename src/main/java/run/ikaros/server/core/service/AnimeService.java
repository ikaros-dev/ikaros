package run.ikaros.server.core.service;

import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;
import run.ikaros.server.entity.AnimeEntity;
import run.ikaros.server.model.dto.AnimeDTO;
import run.ikaros.server.params.SearchAnimeDTOSParams;
import run.ikaros.server.result.PagingWrap;

import javax.annotation.Nonnull;

/**
 * @author li-guohao
 */
public interface AnimeService extends CrudService<AnimeEntity, Long> {

    @Nonnull
    @Transactional
    AnimeDTO saveAnimeDTO(@Nonnull AnimeDTO animeDTO);

    @Nonnull
    AnimeDTO findAnimeDTOById(@Nonnull Long id);

    @Nonnull
    PagingWrap<AnimeDTO> findAnimeDTOS(@Nonnull SearchAnimeDTOSParams searchAnimeDTOSParams);

    @Nullable
    AnimeEntity findByBgmTvId(@Nonnull Long bgmtvId);
}
