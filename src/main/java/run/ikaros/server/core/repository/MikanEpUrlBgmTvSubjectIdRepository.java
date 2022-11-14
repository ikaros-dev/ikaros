package run.ikaros.server.core.repository;

import run.ikaros.server.entity.MikanEpUrlBgmTvSubjectIdEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface MikanEpUrlBgmTvSubjectIdRepository
    extends BaseRepository<MikanEpUrlBgmTvSubjectIdEntity, Long> {

    boolean existsByMikanEpisodeUrl(@Nonnull String mikanEpisodeUrl);

    @Nullable
    MikanEpUrlBgmTvSubjectIdEntity findByMikanEpisodeUrl(@Nonnull String mikanEpisodeUrl);

}
