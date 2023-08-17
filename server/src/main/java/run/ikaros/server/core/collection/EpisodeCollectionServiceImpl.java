package run.ikaros.server.core.collection;

import static run.ikaros.server.infra.utils.ReactiveBeanUtils.copyProperties;

import java.time.LocalDateTime;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.AppConst;
import run.ikaros.api.core.collection.EpisodeCollection;
import run.ikaros.server.store.entity.EpisodeCollectionEntity;
import run.ikaros.server.store.repository.EpisodeCollectionRepository;
import run.ikaros.server.store.repository.EpisodeRepository;

@Slf4j
@Service
public class EpisodeCollectionServiceImpl implements EpisodeCollectionService {

    private final EpisodeCollectionRepository episodeCollectionRepository;
    private final EpisodeRepository episodeRepository;

    public EpisodeCollectionServiceImpl(EpisodeCollectionRepository episodeCollectionRepository,
                                        EpisodeRepository episodeRepository) {
        this.episodeCollectionRepository = episodeCollectionRepository;
        this.episodeRepository = episodeRepository;
    }

    @Override
    public Mono<EpisodeCollection> create(Long userId, Long episodeId) {
        Assert.isTrue(userId >= 0, "'userId' must >= 0");
        Assert.isTrue(episodeId >= 0, "'episodeId' must >= 0");
        return episodeCollectionRepository.findByUserIdAndEpisodeId(userId, episodeId)
            .switchIfEmpty(episodeCollectionRepository
                .save(EpisodeCollectionEntity.builder()
                    .userId(userId)
                    .episodeId(episodeId)
                    .finish(false)
                    .build())
                .doOnSuccess(entity -> log.info(
                    "Create new episode collection for userId=[{}] and episodeId=[{}]",
                    userId, episodeId)))
            .flatMap(this::entityTo);
    }

    @Override
    public Mono<EpisodeCollection> remove(Long userId, Long episodeId) {
        Assert.isTrue(userId >= 0, "'userId' must >= 0");
        Assert.isTrue(episodeId >= 0, "'episodeId' must >= 0");
        return episodeCollectionRepository.findByUserIdAndEpisodeId(userId, episodeId)
            .flatMap(entity -> episodeCollectionRepository.delete(entity)
                .doOnSuccess(unused -> log.info(
                    "Remove episode collection for userId=[{}] and episodeId=[{}]",
                    userId, episodeId))
                .then(Mono.just(entity)))
            .flatMap(this::entityTo);
    }

    private Mono<EpisodeCollection> entityTo(EpisodeCollectionEntity entity) {
        return Mono.justOrEmpty(entity)
            .flatMap(episodeCollectionEntity ->
                copyProperties(episodeCollectionEntity, new EpisodeCollection()))
            .flatMap(episodeCollection ->
                episodeRepository.findById(episodeCollection.getEpisodeId())
                    .flatMap(episodeEntity -> copyProperties(episodeEntity, episodeCollection)
                    ));
    }

    @Override
    public Mono<EpisodeCollection> findByUserIdAndEpisodeId(Long userId, Long episodeId) {
        Assert.isTrue(userId >= 0, "userId must >= 0");
        Assert.isTrue(episodeId >= 0, "episodeId must >= 0");
        return episodeCollectionRepository.findByUserIdAndEpisodeId(userId, episodeId)
            .flatMap(this::entityTo);
    }

    @Override
    public Mono<Void> updateEpisodeCollectionProgress(Long userId, Long episodeId,
                                                      Long progress) {
        Assert.isTrue(userId >= 0, "userId must >= 0");
        Assert.isTrue(episodeId > 0, "episodeId must >= 0");
        Assert.isTrue(progress > 0, "progress must > 0");

        return episodeCollectionRepository.findByUserIdAndEpisodeId(userId, episodeId)
            .switchIfEmpty(episodeCollectionRepository.save(EpisodeCollectionEntity.builder()
                    .userId(userId).episodeId(episodeId).progress(progress)
                    .finish(false).updateTime(LocalDateTime.now())
                    .build())
                .doOnSuccess(entity ->
                    log.info("Create new episode collection for episodeId=[{}] and userId=[{}]",
                        episodeId, userId)))
            .map(episodeCollectionEntity -> episodeCollectionEntity.setProgress(progress))
            .flatMap(
                episodeCollectionEntity -> episodeCollectionRepository.save(episodeCollectionEntity)
                    .doOnSuccess(episodeCollectionEntity1 -> log.info(
                        "Update episode collection process for episodeId=[{}] and userId=[{}]",
                        episodeId, userId)))
            .then();
    }

    @Override
    public Mono<Void> updateEpisodeCollection(Long userId, Long episodeId, Long progress,
                                              Long duration) {
        Assert.isTrue(userId >= 0, "userId must >= 0");
        Assert.isTrue(episodeId > 0, "episodeId must >= 0");
        Assert.isTrue(progress > 0, "progress must > 0");

        if (Objects.isNull(duration) || duration <= 0) {
            return updateEpisodeCollectionProgress(userId, episodeId, progress);
        }

        return episodeCollectionRepository.findByUserIdAndEpisodeId(userId, episodeId)
            .switchIfEmpty(episodeCollectionRepository.save(EpisodeCollectionEntity.builder()
                    .userId(userId).episodeId(episodeId).progress(progress).duration(duration)
                    .finish(((double) progress / duration) >= AppConst.EPISODE_FINISH)
                    .updateTime(LocalDateTime.now())
                    .build())
                .doOnSuccess(entity ->
                    log.info("Create new episode collection for episodeId=[{}] and userId=[{}]",
                        episodeId, userId)))
            .map(episodeCollectionEntity ->
                episodeCollectionEntity.setProgress(progress).setDuration(duration)
                    .setUpdateTime(LocalDateTime.now())
                    .setFinish(((double) progress / duration) >= AppConst.EPISODE_FINISH))
            .flatMap(
                episodeCollectionEntity -> episodeCollectionRepository.save(episodeCollectionEntity)
                    .doOnSuccess(episodeCollectionEntity1 -> log.info(
                        "Update episode collection for episodeId=[{}] and userId=[{}]",
                        episodeId, userId)))
            .then();
    }

    @Override
    public Mono<Void> updateEpisodeCollectionFinish(Long userId, Long episodeId,
                                                    Boolean finish) {
        Assert.isTrue(userId >= 0, "userId must >= 0");
        Assert.isTrue(episodeId >= 0, "episodeId must >= 0");
        Assert.notNull(finish, "'finish' must not null.");
        return episodeCollectionRepository.findByUserIdAndEpisodeId(userId, episodeId)
            .map(episodeCollectionEntity -> episodeCollectionEntity.setFinish(finish))
            .flatMap(
                episodeCollectionEntity -> episodeCollectionRepository.save(episodeCollectionEntity)
                    .doOnSuccess(episodeCollectionEntity1 -> log.info(
                        "Update episode collection finish for episodeId=[{}] and userId=[{}]",
                        episodeId, userId)))
            .then();
    }
}
