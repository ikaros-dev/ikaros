package run.ikaros.server.core.collection;

import static run.ikaros.server.infra.utils.ReactiveBeanUtils.copyProperties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.collection.SubjectCollection;
import run.ikaros.api.infra.exception.subject.SubjectNotFoundException;
import run.ikaros.api.store.enums.CollectionType;
import run.ikaros.server.store.entity.BaseEntity;
import run.ikaros.server.store.entity.EpisodeCollectionEntity;
import run.ikaros.server.store.entity.SubjectCollectionEntity;
import run.ikaros.server.store.repository.EpisodeCollectionRepository;
import run.ikaros.server.store.repository.EpisodeRepository;
import run.ikaros.server.store.repository.SubjectCollectionRepository;
import run.ikaros.server.store.repository.SubjectRepository;

@Slf4j
@Service
public class SubjectCollectionImpl implements SubjectCollectionService {
    private final SubjectCollectionRepository subjectCollectionRepository;
    private final EpisodeRepository episodeRepository;
    private final EpisodeCollectionRepository episodeCollectionRepository;
    private final SubjectRepository subjectRepository;

    /**
     * Construct.
     */
    public SubjectCollectionImpl(SubjectCollectionRepository subjectCollectionRepository,
                                 EpisodeCollectionRepository episodeCollectionRepository,
                                 EpisodeRepository episodeRepository,
                                 SubjectRepository subjectRepository) {
        this.subjectCollectionRepository = subjectCollectionRepository;
        this.episodeCollectionRepository = episodeCollectionRepository;
        this.episodeRepository = episodeRepository;
        this.subjectRepository = subjectRepository;
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
            .switchIfEmpty(subjectCollectionRepository.save(SubjectCollectionEntity.builder()
                .userId(userId)
                .subjectId(subjectId)
                .isPrivate(isPrivate)
                .type(type)
                .mainEpisodeProgress(0)
                .build()).doOnSuccess(subjectCollectionEntity ->
                log.info("Create new subject collection for userId={} and subjectId={}",
                    userId, subjectId)))
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
        return subjectCollectionRepository.findByUserIdAndSubjectId(userId, subjectId)
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
        return subjectCollectionRepository.findByUserIdAndSubjectId(userId, subjectId)
            .flatMap(subjectCollectionEntity ->
                copyProperties(subjectCollectionEntity, new SubjectCollection()))
            .flatMap(subjectCollection ->
                subjectRepository.findById(subjectId)
                    .switchIfEmpty(Mono.error(
                        new SubjectNotFoundException("Subject not found for id: " + subjectId)))
                    .flatMap(subjectEntity -> copyProperties(subjectEntity, subjectCollection)));
    }
}
