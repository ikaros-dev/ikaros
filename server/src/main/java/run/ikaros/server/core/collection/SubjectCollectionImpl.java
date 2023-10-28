package run.ikaros.server.core.collection;

import static run.ikaros.server.infra.utils.ReactiveBeanUtils.copyProperties;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.collection.SubjectCollection;
import run.ikaros.api.core.collection.event.EpisodeCollectionFinishChangeEvent;
import run.ikaros.api.core.collection.event.SubjectCollectEvent;
import run.ikaros.api.core.collection.event.SubjectUnCollectEvent;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.infra.exception.subject.SubjectNotFoundException;
import run.ikaros.api.infra.exception.user.UserNotFoundException;
import run.ikaros.api.store.enums.CollectionType;
import run.ikaros.api.store.enums.EpisodeGroup;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.store.entity.BaseEntity;
import run.ikaros.server.store.entity.EpisodeCollectionEntity;
import run.ikaros.server.store.entity.SubjectCollectionEntity;
import run.ikaros.server.store.repository.EpisodeCollectionRepository;
import run.ikaros.server.store.repository.EpisodeRepository;
import run.ikaros.server.store.repository.SubjectCollectionRepository;
import run.ikaros.server.store.repository.SubjectRepository;
import run.ikaros.server.store.repository.UserRepository;

@Slf4j
@Service
public class SubjectCollectionImpl implements SubjectCollectionService {
    private final SubjectCollectionRepository subjectCollectionRepository;
    private final EpisodeRepository episodeRepository;
    private final EpisodeCollectionRepository episodeCollectionRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final R2dbcEntityTemplate template;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Construct.
     */
    public SubjectCollectionImpl(SubjectCollectionRepository subjectCollectionRepository,
                                 EpisodeCollectionRepository episodeCollectionRepository,
                                 EpisodeRepository episodeRepository,
                                 SubjectRepository subjectRepository,
                                 UserRepository userRepository, R2dbcEntityTemplate template,
                                 ApplicationEventPublisher applicationEventPublisher) {
        this.subjectCollectionRepository = subjectCollectionRepository;
        this.episodeCollectionRepository = episodeCollectionRepository;
        this.episodeRepository = episodeRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
        this.template = template;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    private Mono<Boolean> checkUserIdExists(Long userId) {
        return userRepository.existsById(userId)
            .filter(exists -> exists)
            .switchIfEmpty(
                Mono.error(new UserNotFoundException("User not found for id=" + userId)));
    }

    private Mono<Boolean> checkSubjectIdExists(Long subjectId) {
        return subjectRepository.existsById(subjectId)
            .filter(exists -> exists)
            .switchIfEmpty(
                Mono.error(new SubjectNotFoundException("Subject not found for id=" + subjectId)));
    }

    @Override
    public Mono<Void> collect(Long userId, Long subjectId,
                              CollectionType type, Boolean isPrivate) {
        Assert.isTrue(userId >= 0, "'userId' must >= 0");
        Assert.isTrue(subjectId >= 0, "'subjectId' must >= 0");
        Assert.notNull(type, "'type' must not null");
        Assert.notNull(isPrivate, "'isPrivate' must not null");
        return findCollection(userId, subjectId)
            .flatMap(subjectCollection ->
                subjectCollectionRepository.findByUserIdAndSubjectId(userId, subjectId))
            .switchIfEmpty(
                subjectCollectionRepository.save(SubjectCollectionEntity.builder()
                    .userId(userId)
                    .subjectId(subjectId)
                    .isPrivate(isPrivate)
                    .type(type)
                    .mainEpisodeProgress(0)
                    .build()).doOnSuccess(subjectCollectionEntity ->
                    log.info("Create new subject collection for userId={} and subjectId={}",
                        userId, subjectId)))
            .map(subjectCollectionEntity -> subjectCollectionEntity
                .setType(type).setIsPrivate(isPrivate))
            .flatMap(entity -> subjectCollectionRepository.save(entity)
                .doOnSuccess(ent -> {
                    SubjectCollectEvent event =
                        new SubjectCollectEvent(this, SubjectCollection.builder()
                            .userId(ent.getUserId())
                            .subjectId(ent.getSubjectId())
                            .isPrivate(ent.getIsPrivate())
                            .type(ent.getType())
                            .build());
                    log.debug("Publish SubjectCollectEvent after collect subject"
                        + "for userId=[{}] and subjectId=[{}]", userId, subjectId);
                    applicationEventPublisher.publishEvent(event);
                }))

            .map(SubjectCollectionEntity::getSubjectId)
            .flatMapMany(episodeRepository::findAllBySubjectId)
            .map(BaseEntity::getId)
            .flatMap(episodeId ->
                episodeCollectionRepository.findByUserIdAndEpisodeId(userId, episodeId)
                    .switchIfEmpty(
                        episodeCollectionRepository.save(EpisodeCollectionEntity.builder()
                                .userId(userId)
                                .subjectId(subjectId)
                                .episodeId(episodeId)
                                .finish(false)
                                .build())
                            .doOnSuccess(entity -> log.info(
                                "Create new episode collection "
                                    + "for userId is [{}] and episode id is [{}]",
                                userId, entity.getEpisodeId())))
            )
            .then();
    }

    @Override
    public Mono<Void> collect(Long userId, Long subjectId, CollectionType type) {
        return collect(userId, subjectId, type, false);
    }

    @Override
    public Mono<Void> unCollect(Long userId, Long subjectId) {
        Assert.isTrue(userId >= 0, "'userId' must >= 0");
        Assert.isTrue(subjectId >= 0, "'subjectId' must >= 0");
        return checkUserIdExists(userId)
            .flatMap(exists -> findCollection(userId, subjectId))
            .flatMap(subjectCollection ->
                subjectCollectionRepository.findByUserIdAndSubjectId(userId, subjectId))
            .flatMap(subjectCollectionEntity ->
                subjectCollectionRepository.delete(subjectCollectionEntity)
                    .doOnSuccess(unused -> {
                        log.debug("Delete exists subject collection "
                                + "for userId is [{}] and subjectId is [{}]",
                            userId, subjectId);
                        SubjectUnCollectEvent event =
                            new SubjectUnCollectEvent(this, SubjectCollection.builder()
                                .userId(subjectCollectionEntity.getUserId())
                                .subjectId(subjectCollectionEntity.getSubjectId())
                                .isPrivate(subjectCollectionEntity.getIsPrivate())
                                .type(subjectCollectionEntity.getType())
                                .build());
                        log.debug("Publish SubjectUnCollectEvent after uncollect subject"
                                + "for userId=[{}] and subjectId=[{}]", userId, subjectId);
                        applicationEventPublisher.publishEvent(event);
                    }))
            // not delete user episode collection record for retain watch progress.
            // .thenMany(episodeRepository.findAllBySubjectId(subjectId))
            // .map(BaseEntity::getId)
            // .flatMap(epId ->
            //     episodeCollectionRepository.findByUserIdAndEpisodeId(userId, epId)
            //         .flatMap(entity -> episodeCollectionRepository.delete(entity)
            //             .doOnSuccess(unused -> log.info(
            //                 "Delete exists episode collection "
            //                     + "for userId is [{}] and episode id is [{}]",
            //                 userId, entity.getEpisodeId()))))
            .then();
    }

    @Override
    public Mono<SubjectCollection> findCollection(Long userId, Long subjectId) {
        Assert.isTrue(userId >= 0, "'userId' must >= 0");
        Assert.isTrue(subjectId >= 0, "'subjectId' must >= 0");
        return checkUserIdExists(userId)
            .then(subjectRepository.findById(subjectId))
            .switchIfEmpty(Mono.error(
                new SubjectNotFoundException("Subject not found for id: " + subjectId)))
            .flatMap(subjectEntity -> copyProperties(subjectEntity, new SubjectCollection()))
            .flatMap(subjectCollection ->
                subjectCollectionRepository.findByUserIdAndSubjectId(userId, subjectId)
                    .flatMap(subjectCollectionEntity -> copyProperties(subjectCollectionEntity,
                        subjectCollection)));
    }

    @Override
    public Mono<PagingWrap<SubjectCollection>> findCollections(Long userId, Integer page,
                                                               Integer size) {
        Assert.isTrue(userId >= 0, "'userId' must >= 0");
        Assert.isTrue(page > 0, "'page' must > 0");
        Assert.isTrue(size > 0, "'size' must > 0");
        return findCollections(userId, page, size, null, null);
    }

    @Override
    public Mono<PagingWrap<SubjectCollection>> findCollections(Long userId, Integer page,
                                                               Integer size,
                                                               CollectionType type,
                                                               Boolean isPrivate) {
        Assert.isTrue(userId >= 0, "'userId' must >= 0");
        Assert.isTrue(page > 0, "'page' must > 0");
        Assert.isTrue(size > 0, "'size' must > 0");

        final PageRequest pageRequest = PageRequest.of(page - 1, size);

        Criteria criteria =
            Criteria.where("user_id").is(userId);

        if (Objects.nonNull(type)) {
            criteria = criteria.and("type").is(type);
        }

        if (Objects.nonNull(isPrivate)) {
            criteria = criteria.and("is_private").is(isPrivate);
        }

        Query query = Query.query(criteria)
            .sort(Sort.by(Sort.Order.desc("subject_id")))
            .with(pageRequest);

        Flux<SubjectCollectionEntity> subjectCollectionEntityFlux =
            template.select(query, SubjectCollectionEntity.class);
        Mono<Long> countMono = template.count(query, SubjectCollectionEntity.class);

        return subjectCollectionEntityFlux
            .map(SubjectCollectionEntity::getSubjectId)
            .flatMap(subjectId -> findCollection(userId, subjectId))
            .collectList()
            .flatMap(subjectCollectionEntities -> countMono
                .map(count -> new PagingWrap<>(page, size, count, subjectCollectionEntities)));
    }

    @Override
    public Mono<Void> updateMainEpisodeProgress(Long userId, Long subjectId, Integer progress) {
        Assert.isTrue(userId >= 0, "'userId' must >= 0");
        Assert.isTrue(subjectId >= 0, "'subjectId' must >= 0");
        Assert.isTrue(progress >= 0, "'progress' must >= 0");

        return checkUserIdExists(userId)
            .then(checkSubjectIdExists(subjectId))
            .then(subjectCollectionRepository.findByUserIdAndSubjectId(userId, subjectId))
            .switchIfEmpty(Mono.error(new NotFoundException(
                "Subject collection not found for userId=" + userId + " subjectId=" + subjectId)))
            .map(entity -> entity.setMainEpisodeProgress(progress))
            .flatMap(subjectCollectionRepository::save)
            .flatMapMany(entity -> episodeRepository.findAllBySubjectId(subjectId))
            .filter(episodeEntity -> episodeEntity.getGroup() == EpisodeGroup.MAIN)
            .filter(episodeEntity -> progress >= episodeEntity.getSequence())
            .map(BaseEntity::getId)
            .flatMap(episodeId -> episodeCollectionRepository.findByUserIdAndEpisodeId(userId,
                episodeId))
            .map(entity -> entity.setFinish(true))
            .flatMap(entity -> episodeCollectionRepository.save(entity)
                .doOnSuccess(e -> {
                    log.debug(
                        "Mark episode collection finish is true for userId={} and episode={}",
                        userId, entity.getEpisodeId());
                    EpisodeCollectionFinishChangeEvent event =
                        new EpisodeCollectionFinishChangeEvent(this, userId,
                            e.getEpisodeId(), true);
                    event.setSubjectId(e.getSubjectId());
                    log.debug("Publish EpisodeCollectionFinishChangeEvent "
                            + "for userId=[{}] and episodeId=[{}] and finish=[{}]",
                        userId, e.getEpisodeId(), true);
                    applicationEventPublisher.publishEvent(event);
                }))
            .then();
    }
}
