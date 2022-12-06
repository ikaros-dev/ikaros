package run.ikaros.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import run.ikaros.server.core.repository.UserSubscribeRepository;
import run.ikaros.server.core.service.AnimeService;
import run.ikaros.server.core.service.UserSubscribeService;
import run.ikaros.server.core.tripartite.bgmtv.service.BgmTvService;
import run.ikaros.server.entity.AnimeEntity;
import run.ikaros.server.entity.UserSubscribeEntity;
import run.ikaros.server.enums.SubscribeProgress;
import run.ikaros.server.enums.SubscribeType;
import run.ikaros.server.model.dto.AnimeDTO;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@Slf4j
@Service
public class UserSubscribeServiceImpl
    extends AbstractCrudService<UserSubscribeEntity, Long>
    implements UserSubscribeService {
    private final UserSubscribeRepository userSubscribeRepository;
    private final AnimeService animeService;
    private final BgmTvService bgmTvService;

    public UserSubscribeServiceImpl(UserSubscribeRepository userSubscribeRepository,
                                    AnimeService animeService, BgmTvService bgmTvService) {
        super(userSubscribeRepository);
        this.userSubscribeRepository = userSubscribeRepository;
        this.animeService = animeService;
        this.bgmTvService = bgmTvService;
    }


    @Override
    public void saveUserAnimeSubscribe(@Nonnull Long userId, @Nonnull Long animeId,
                                       @Nonnull String progress,
                                       @Nullable String additional) {
        AssertUtils.notNull(userId, "userId");
        AssertUtils.notNull(animeId, "animeId");
        AssertUtils.notBlank(progress, "progress");

        Optional<UserSubscribeEntity> userSubscribeEntityOptional =
            userSubscribeRepository.findByUserIdAndTypeAndTargetId(userId,
                SubscribeType.ANIME, animeId);
        if (userSubscribeEntityOptional.isEmpty()) {
            UserSubscribeEntity userSubscribeEntity = new UserSubscribeEntity();
            userSubscribeEntity.setUserId(userId);
            userSubscribeEntity.setType(SubscribeType.ANIME);
            userSubscribeEntity.setProgress(SubscribeProgress.valueOf(progress));
            userSubscribeEntity.setTargetId(animeId);
            if (StringUtils.isNotBlank(additional)) {
                userSubscribeEntity.setAdditional(additional);
            }
            userSubscribeRepository.save(userSubscribeEntity);
        } else {
            UserSubscribeEntity userSubscribeEntity = userSubscribeEntityOptional.get();
            userSubscribeEntity.setProgress(SubscribeProgress.valueOf(progress));
            if (!userSubscribeEntity.getStatus()) {
                userSubscribeEntity.setStatus(true);
            }
            if (StringUtils.isNotBlank(additional)) {
                userSubscribeEntity.setAdditional(additional);
            }
            userSubscribeRepository.save(userSubscribeEntity);
        }
    }

    @Override
    public boolean findUserAnimeSubscribeStatus(@Nonnull Long userId, @Nonnull Long animeId) {
        AssertUtils.notNull(userId, "userId");
        AssertUtils.notNull(animeId, "animeId");
        Optional<UserSubscribeEntity> userSubscribeEntityOptional =
            userSubscribeRepository.findByUserIdAndTypeAndTargetId(userId,
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
        Optional<UserSubscribeEntity> userSubscribeEntityOptional =
            userSubscribeRepository.findByUserIdAndTypeAndTargetId(userId,
                SubscribeType.ANIME, animeId);
        if (userSubscribeEntityOptional.isPresent()) {
            UserSubscribeEntity userSubscribeEntity = userSubscribeEntityOptional.get();
            if (userSubscribeEntity.getStatus()) {
                userSubscribeEntity.setStatus(false);
                userSubscribeRepository.save(userSubscribeEntity);
            }
        }
    }
}
