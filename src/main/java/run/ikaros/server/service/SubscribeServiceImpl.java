package run.ikaros.server.service;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import run.ikaros.server.core.repository.SubscribeRepository;
import run.ikaros.server.core.service.AnimeService;
import run.ikaros.server.core.service.SubscribeService;
import run.ikaros.server.core.service.UserService;
import run.ikaros.server.core.tripartite.bgmtv.service.BgmTvService;
import run.ikaros.server.entity.AnimeEntity;
import run.ikaros.server.entity.SubscribeEntity;
import run.ikaros.server.enums.SubscribeProgress;
import run.ikaros.server.enums.SubscribeType;
import run.ikaros.server.event.SubscribeBgmTvSubjectEvent;
import run.ikaros.server.exceptions.RuntimeIkarosException;
import run.ikaros.server.model.dto.AnimeDTO;
import run.ikaros.server.utils.AssertUtils;

import jakarta.annotation.Nonnull;
import run.ikaros.server.utils.StringUtils;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class SubscribeServiceImpl
    extends AbstractCrudService<SubscribeEntity, Long>
    implements SubscribeService, ApplicationContextAware {
    private final SubscribeRepository subscribeRepository;
    private final AnimeService animeService;
    private final BgmTvService bgmTvService;

    private ApplicationContext applicationContext;

    public SubscribeServiceImpl(
        SubscribeRepository subscribeRepository, AnimeService animeService,
        BgmTvService bgmTvService) {
        super(subscribeRepository);
        this.subscribeRepository = subscribeRepository;
        this.animeService = animeService;
        this.bgmTvService = bgmTvService;
    }

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext)
        throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void subscribeBgmTvSubject(@Nonnull Long bgmTvSubjectId) {
        AssertUtils.isPositive(bgmTvSubjectId, "bgmtv subject id");

        AnimeDTO animeDTO = bgmTvService.reqBgmtvSubject(bgmTvSubjectId);
        if (animeDTO == null) {
            throw new RuntimeIkarosException("not exists for target bgmtv subject id: "
                + bgmTvSubjectId);
        }
        Long animeId = animeDTO.getId();

        SubscribeEntity subscribeEntity = new SubscribeEntity();
        subscribeEntity.setType(SubscribeType.ANIME);
        subscribeEntity.setTargetId(animeId);
        subscribeEntity.setUserId(UserService.getCurrentLoginUserUid());
        save(subscribeEntity);

        SubscribeBgmTvSubjectEvent subscribeBgmTvSubjectEvent =
            new SubscribeBgmTvSubjectEvent(this, bgmTvSubjectId, animeId);
        applicationContext.publishEvent(subscribeBgmTvSubjectEvent);
        log.debug("publish SubscribeBgmTvSubjectEvent");
    }



    @Override
    public void saveUserAnimeSubscribe(@Nonnull Long userId, @Nonnull Long animeId,
                                       @Nullable String progress,
                                       @Nullable String additional) {
        AssertUtils.notNull(userId, "userId");
        AssertUtils.notNull(animeId, "animeId");

        Optional<SubscribeEntity> userSubscribeEntityOptional =
            subscribeRepository.findByUserIdAndTypeAndTargetId(userId,
                SubscribeType.ANIME, animeId);
        if (userSubscribeEntityOptional.isEmpty()) {
            SubscribeEntity subscribeEntity = new SubscribeEntity();
            subscribeEntity.setUserId(userId);
            subscribeEntity.setType(SubscribeType.ANIME);
            if (StringUtils.isNotBlank(progress)) {
                subscribeEntity.setProgress(SubscribeProgress.valueOf(progress));
            }
            subscribeEntity.setTargetId(animeId);
            if (StringUtils.isNotBlank(additional)) {
                subscribeEntity.setAdditional(additional);
            }
            subscribeRepository.save(subscribeEntity);
        } else {
            SubscribeEntity subscribeEntity = userSubscribeEntityOptional.get();
            if (StringUtils.isNotBlank(progress)) {
                subscribeEntity.setProgress(SubscribeProgress.valueOf(progress));
            }
            if (!subscribeEntity.getStatus()) {
                subscribeEntity.setStatus(true);
            }
            if (StringUtils.isNotBlank(additional)) {
                subscribeEntity.setAdditional(additional);
            }
            subscribeRepository.save(subscribeEntity);
        }
    }

    @Override
    public boolean findUserAnimeSubscribeStatus(@Nonnull Long userId, @Nonnull Long animeId) {
        AssertUtils.notNull(userId, "userId");
        AssertUtils.notNull(animeId, "animeId");
        Optional<SubscribeEntity> userSubscribeEntityOptional =
            subscribeRepository.findByUserIdAndTypeAndTargetId(userId,
                SubscribeType.ANIME, animeId);
        return userSubscribeEntityOptional.isPresent()
            && userSubscribeEntityOptional.get().getStatus();
    }

    @Override
    public void saveUserAnimeSubscribeByBgmTvSubjectId(@Nonnull Long userId,
                                                       @Nonnull Long bgmtvSubjectId) {
        AssertUtils.notNull(userId, "userId");
        AssertUtils.notNull(bgmtvSubjectId, "bgmtvSubjectId");
        AnimeEntity animeEntity = animeService.findByBgmTvId(bgmtvSubjectId);
        Long animeId;
        if (animeEntity == null) {
            AnimeDTO animeDTO = bgmTvService.reqBgmtvSubject(bgmtvSubjectId);
            animeId = animeDTO.getId();
        } else {
            animeId = animeEntity.getId();
        }
        saveUserAnimeSubscribe(userId, animeId, SubscribeProgress.WISH.name(), null);
    }

    @Override
    public void deleteUserAnimeSubscribe(@Nonnull Long userId, @Nonnull Long animeId) {
        AssertUtils.notNull(userId, "userId");
        AssertUtils.notNull(animeId, "animeId");
        Optional<SubscribeEntity> userSubscribeEntityOptional =
            subscribeRepository.findByUserIdAndTypeAndTargetId(userId,
                SubscribeType.ANIME, animeId);
        if (userSubscribeEntityOptional.isPresent()) {
            SubscribeEntity userSubscribeEntity = userSubscribeEntityOptional.get();
            if (userSubscribeEntity.getStatus()) {
                userSubscribeEntity.setStatus(false);
                subscribeRepository.save(userSubscribeEntity);
            }
        }
    }

    @Nonnull
    @Override
    public List<SubscribeEntity> findByUserIdAndStatus(@Nonnull Long userId,
                                                           @Nonnull Boolean status) {
        AssertUtils.notNull(userId, "userId");
        AssertUtils.notNull(status, "status");
        return subscribeRepository.findByUserIdAndStatus(userId, status);
    }

    @Override
    public void saveUserSubscribeWithBatchByAnimeIdArr(@Nonnull Long[] animeIdArr) {
        AssertUtils.notNull(animeIdArr, "animeIdArr");
        Long uid = UserService.getCurrentLoginUserUid();
        for (Long animeId : animeIdArr) {
            saveUserAnimeSubscribe(uid, animeId, null, null);
        }
    }

}
