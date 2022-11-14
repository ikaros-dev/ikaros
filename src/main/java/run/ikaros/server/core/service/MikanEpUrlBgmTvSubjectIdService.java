package run.ikaros.server.core.service;

import run.ikaros.server.entity.MikanEpUrlBgmTvSubjectIdEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface MikanEpUrlBgmTvSubjectIdService
    extends CrudService<MikanEpUrlBgmTvSubjectIdEntity, Long> {
    boolean existsByMikanEpisodeUrl(@Nonnull String mikanEpisodeUrl);

    @Nullable
    MikanEpUrlBgmTvSubjectIdEntity findByMikanEpisodeUrl(@Nonnull String mikanEpisodeUrl);
}
