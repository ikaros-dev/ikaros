package run.ikaros.server.core.service;

import jakarta.annotation.Nullable;
import org.springframework.transaction.annotation.Transactional;
import run.ikaros.server.entity.SubscribeEntity;

import jakarta.annotation.Nonnull;

import java.util.List;

public interface SubscribeService extends CrudService<SubscribeEntity, Long> {

    void subscribeBgmTvSubject(@Nonnull Long bgmTvSubjectId);

    @Transactional
    void saveUserAnimeSubscribe(@Nonnull Long userId, @Nonnull Long animeId,
                                @Nullable String progress,
                                @Nullable String additional);

    boolean findUserAnimeSubscribeStatus(@Nonnull Long userId, @Nonnull Long animeId);

    @Transactional
    void saveUserAnimeSubscribeByBgmTvSubjectId(@Nonnull Long userId, @Nonnull Long bgmtvSubjectId);

    @Transactional
    void deleteUserAnimeSubscribe(@Nonnull Long userId, @Nonnull Long animeId);

    @Nonnull
    List<SubscribeEntity> findByUserIdAndStatus(@Nonnull Long userId, @Nonnull Boolean status);

    @Transactional
    void saveUserSubscribeWithBatchByAnimeIdArr(@Nonnull Long[] animeIdArr);

}
