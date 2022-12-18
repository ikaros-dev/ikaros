package run.ikaros.server.core.service;

import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;
import run.ikaros.server.entity.AnimeEntity;
import run.ikaros.server.model.dto.AnimeDTO;
import run.ikaros.server.params.SearchAnimeDTOSParams;
import run.ikaros.server.result.PagingWrap;

import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

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

    @Nonnull
    Optional<AnimeEntity> findByTitle(@Nonnull String title);

    @Nonnull
    Optional<AnimeEntity> findByTitleCn(@Nonnull String titleCn);

    @Nonnull
    List<AnimeEntity> findByTitleLike(@Nonnull String title);

    @Nonnull
    List<AnimeEntity> findByTitleCnLike(@Nonnull String titleCn);

    @Nullable
    AnimeEntity deleteByIdLogically(@Nonnull Long animeId);

    @Nonnull
    List<AnimeEntity> findAll();

    @Transactional
    void deleteWithBatchLogicallyByIds(@Nonnull Long[] animeIdArray);
}
