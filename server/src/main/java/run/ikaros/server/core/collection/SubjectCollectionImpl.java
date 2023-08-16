package run.ikaros.server.core.collection;

import static run.ikaros.server.infra.utils.ReactiveBeanUtils.copyProperties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.collection.SubjectCollection;
import run.ikaros.api.infra.exception.subject.SubjectNotFoundException;
import run.ikaros.api.infra.exception.user.UserNotFoundException;
import run.ikaros.api.store.enums.CollectionType;
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

    /**
     * Construct.
     */
    public SubjectCollectionImpl(SubjectCollectionRepository subjectCollectionRepository,
                                 EpisodeCollectionRepository episodeCollectionRepository,
                                 EpisodeRepository episodeRepository,
                                 SubjectRepository subjectRepository,
                                 UserRepository userRepository) {
        this.subjectCollectionRepository = subjectCollectionRepository;
        this.episodeCollectionRepository = episodeCollectionRepository;
        this.episodeRepository = episodeRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
    }

    private Mono<Boolean> checkUserIdExists(Long userId) {
        return userRepository.existsById(userId)
            .filter(exists -> exists)
            .switchIfEmpty(
                Mono.error(new UserNotFoundException("User not found for id=" + userId)));
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
            .flatMap(subjectCollectionRepository::save)
            .map(SubjectCollectionEntity::getSubjectId)
            .flatMapMany(episodeRepository::findAllBySubjectId)
            .map(BaseEntity::getId)
            .flatMap(episodeId ->
                episodeCollectionRepository.findByUserIdAndEpisodeId(userId, episodeId)
                    .switchIfEmpty(
                        episodeCollectionRepository.save(EpisodeCollectionEntity.builder()
                                .userId(userId)
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
                    .doOnSuccess(unused -> log.info("Delete exists subject collection "
                            + "for userId is [{}] and subjectId is [{}]",
                        userId, subjectId)))
            .thenMany(episodeRepository.findAllBySubjectId(subjectId))
            .map(BaseEntity::getId)
            .flatMap(epId ->
                episodeCollectionRepository.findByUserIdAndEpisodeId(userId, epId)
                    .flatMap(entity -> episodeCollectionRepository.delete(entity)
                        .doOnSuccess(unused -> log.info(
                            "Delete exists episode collection "
                                + "for userId is [{}] and episode id is [{}]",
                            userId, entity.getEpisodeId()))))
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
    public Mono<PagingWrap<SubjectCollection>> findUserCollections(Long userId, Integer page,
                                                                   Integer size) {
        Assert.isTrue(userId >= 0, "'userId' must >= 0");
        Assert.isTrue(page > 0, "'page' must > 0");
        Assert.isTrue(size > 0, "'size' must > 0");
        return checkUserIdExists(userId)
            .flatMapMany(exists ->
                subjectCollectionRepository.findAllByUserId(userId, PageRequest.of(page - 1, size)))
            .map(SubjectCollectionEntity::getSubjectId)
            .flatMap(subjectId -> findCollection(userId, subjectId))
            .collectList()
            .flatMap(subjectCollections ->
                subjectCollectionRepository.countAllByUserId(userId)
                    .map(total -> new PagingWrap<>(page, size, total, subjectCollections)))
            ;
    }
}
