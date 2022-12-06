package run.ikaros.server.core.service;

import org.springframework.transaction.annotation.Transactional;
import run.ikaros.server.entity.UserSubscribeEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface UserSubscribeService extends CrudService<UserSubscribeEntity, Long> {

    @Transactional
    void saveUserAnimeSubscribe(@Nonnull Long userId, @Nonnull Long animeId,
                                @Nonnull String progress,
                                @Nullable String additional);

    boolean findUserAnimeSubscribeStatus(@Nonnull Long userId, @Nonnull Long animeId);

    @Transactional
    void saveUserAnimeSubscribeByBgmTvSubjectId(@Nonnull Long userId, @Nonnull Long bgmtvSubjectId);

    @Transactional
    void deleteUserAnimeSubscribe(@Nonnull Long userId, @Nonnull Long animeId);
}
