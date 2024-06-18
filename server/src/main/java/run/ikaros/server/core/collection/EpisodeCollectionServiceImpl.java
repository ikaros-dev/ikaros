package run.ikaros.server.core.collection;

import static run.ikaros.api.infra.utils.ReactiveBeanUtils.copyProperties;

import java.time.LocalDateTime;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.AppConst;
import run.ikaros.api.core.collection.EpisodeCollection;
import run.ikaros.api.core.collection.event.EpisodeCollectionFinishChangeEvent;
import run.ikaros.api.infra.exception.subject.EpisodeNotFoundException;
import run.ikaros.server.store.entity.EpisodeCollectionEntity;
import run.ikaros.server.store.entity.EpisodeEntity;
import run.ikaros.server.store.repository.EpisodeCollectionRepository;
import run.ikaros.server.store.repository.EpisodeRepository;

@Slf4j
@Service
public class EpisodeCollectionServiceImpl implements EpisodeCollectionService {

    private final EpisodeCollectionRepository episodeCollectionRepository;
    private final EpisodeRepository episodeRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Construct.
     */
    public EpisodeCollectionServiceImpl(EpisodeCollectionRepository episodeCollectionRepository,
                                        EpisodeRepository episodeRepository,
                                        ApplicationEventPublisher applicationEventPublisher) {
        this.episodeCollectionRepository = episodeCollectionRepository;
        this.episodeRepository = episodeRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public Mono<EpisodeCollection> create(Long userId, Long episodeId) {
        Assert.isTrue(userId >= 0, "'userId' must >= 0");
        Assert.isTrue(episodeId >= 0, "'episodeId' must >= 0");
        return episodeCollectionRepository.findByUserIdAndEpisodeId(userId, episodeId)
            .switchIfEmpty(
                episodeRepository.findById(episodeId)
                    .switchIfEmpty(Mono.error(
                        new EpisodeNotFoundException("episode not found for id: " + episodeId)))
                    .map(EpisodeEntity::getSubjectId)
                    .flatMap(subjectId -> episodeCollectionRepository
                        .save(EpisodeCollectionEntity.builder()
                            .userId(userId)
                            .subjectId(subjectId)
                            .episodeId(episodeId)
                            .finish(false)
                            .build())
                        .doOnSuccess(entity -> log.info(
                            "Create new episode collection for userId=[{}] and episodeId=[{}]",
                            userId, episodeId)))
            )
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
    public Flux<EpisodeCollection> findAllByUserIdAndSubjectId(Long userId, Long subjectId) {
        Assert.isTrue(userId >= 0, "userId must >= 0");
        Assert.isTrue(subjectId > 0, "subjectId must >= 0");
        return episodeCollectionRepository.findAllByUserIdAndSubjectId(userId, subjectId)
            .flatMap(this::entityTo);
    }

    @Override
    public Mono<Void> updateEpisodeCollectionProgress(Long userId, Long episodeId,
                                                      Long progress) {
        Assert.isTrue(userId >= 0, "userId must >= 0");
        Assert.isTrue(episodeId > 0, "episodeId must >= 0");
        Assert.isTrue(progress > 0, "progress must > 0");

        return episodeCollectionRepository.findByUserIdAndEpisodeId(userId, episodeId)
            .switchIfEmpty(createNewEpisodeCollectionEntity(userId, episodeId, progress, null))
            .flatMap(episodeCollectionEntity -> updateSubjectId(episodeId, episodeCollectionEntity))
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

        final boolean finish = ((double) progress / duration) >= AppConst.EPISODE_FINISH;

        return episodeCollectionRepository.findByUserIdAndEpisodeId(userId, episodeId)
            .switchIfEmpty(createNewEpisodeCollectionEntity(userId, episodeId, progress, duration))
            .flatMap(episodeCollectionEntity -> updateSubjectId(episodeId, episodeCollectionEntity))
            .map(episodeCollectionEntity ->
                episodeCollectionEntity.setProgress(progress).setDuration(duration)
                    .setUpdateTime(LocalDateTime.now()))
            .flatMap(
                episodeCollectionEntity -> episodeCollectionRepository.save(episodeCollectionEntity)
                    .doOnSuccess(episodeCollectionEntity1 -> log.info(
                        "Update episode collection for episodeId=[{}] and userId=[{}]",
                        episodeId, userId)))
            .flatMap(entity -> updateEpisodeCollectionFinish(entity.getUserId(),
                entity.getEpisodeId(), finish))
            .then();
    }

    private Mono<EpisodeCollectionEntity> createNewEpisodeCollectionEntity(Long userId,
                                                                           Long episodeId,
                                                                           Long progress,
                                                                           Long duration) {
        return episodeRepository.findById(episodeId)
            .switchIfEmpty(
                Mono.error(new EpisodeNotFoundException("Episode not found for id: " + episodeId)))
            .map(EpisodeEntity::getSubjectId)
            .flatMap(subjectId ->
                episodeCollectionRepository.save(EpisodeCollectionEntity.builder()
                        .userId(userId).subjectId(subjectId)
                        .episodeId(episodeId).progress(progress).duration(duration)
                        .finish(((double) progress / duration) >= AppConst.EPISODE_FINISH)
                        .updateTime(LocalDateTime.now())
                        .build())
                    .doOnSuccess(entity ->
                        log.info(
                            "Create new episode collection for episodeId=[{}] and userId=[{}]",
                            episodeId, userId)));
    }

    private Mono<EpisodeCollectionEntity> updateSubjectId(
        Long episodeId, EpisodeCollectionEntity episodeCollectionEntity) {
        return episodeRepository.findById(episodeId)
            .switchIfEmpty(
                Mono.error(new EpisodeNotFoundException("Episode not found for id: " + episodeId)))
            .map(EpisodeEntity::getSubjectId)
            .map(episodeCollectionEntity::setSubjectId);
    }

    @Override
    public Mono<Void> updateEpisodeCollectionFinish(Long userId, Long episodeId,
                                                    Boolean finish) {
        Assert.isTrue(userId >= 0, "userId must >= 0");
        Assert.isTrue(episodeId >= 0, "episodeId must >= 0");
        Assert.notNull(finish, "'finish' must not null.");
        return episodeCollectionRepository.findByUserIdAndEpisodeId(userId, episodeId)
            .filter(entity -> !finish.equals(entity.getFinish()))
            .map(episodeCollectionEntity ->
                episodeCollectionEntity.setFinish(finish)
                    .setUpdateTime(LocalDateTime.now()))
            .flatMap(
                episodeCollectionEntity -> episodeCollectionRepository.save(episodeCollectionEntity)
                    .doOnSuccess(episodeCollectionEntity1 -> {
                        log.debug(
                            "Update episode collection finish for episodeId=[{}] and userId=[{}]",
                            episodeId, userId);
                        EpisodeCollectionFinishChangeEvent event =
                            new EpisodeCollectionFinishChangeEvent(this, userId, episodeId, finish);
                        event.setSubjectId(episodeCollectionEntity1.getSubjectId());
                        log.debug("Publish EpisodeCollectionFinishChangeEvent "
                                + "for userId=[{}] and episodeId=[{}] and finish=[{}].",
                            userId, episodeCollectionEntity1, finish);
                        applicationEventPublisher.publishEvent(event);
                    }))
            .then();
    }
}
