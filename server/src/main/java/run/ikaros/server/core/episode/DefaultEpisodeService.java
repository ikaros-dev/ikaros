package run.ikaros.server.core.episode;

import static run.ikaros.api.infra.utils.ReactiveBeanUtils.copyProperties;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.cache.CacheEvict;
import run.ikaros.api.cache.CachePut;
import run.ikaros.api.cache.Cacheable;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.EpisodeResource;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.api.store.enums.EpisodeGroup;
import run.ikaros.server.store.entity.EpisodeEntity;
import run.ikaros.server.store.repository.AttachmentReferenceRepository;
import run.ikaros.server.store.repository.AttachmentRepository;
import run.ikaros.server.store.repository.EpisodeRepository;

@Slf4j
@Service
public class DefaultEpisodeService implements EpisodeService {
    private final EpisodeRepository episodeRepository;
    private final AttachmentReferenceRepository attachmentReferenceRepository;
    private final AttachmentRepository attachmentRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final DatabaseClient databaseClient;

    /**
     * Construct.
     */
    public DefaultEpisodeService(EpisodeRepository episodeRepository,
                                 AttachmentReferenceRepository attachmentReferenceRepository,
                                 AttachmentRepository attachmentRepository,
                                 ApplicationEventPublisher applicationEventPublisher,
                                 DatabaseClient databaseClient) {
        this.episodeRepository = episodeRepository;
        this.attachmentReferenceRepository = attachmentReferenceRepository;
        this.attachmentRepository = attachmentRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.databaseClient = databaseClient;
    }


    @Override
    @CachePut(value = "episodeWithId", key = "#episode.id")
    public Mono<Episode> save(Episode episode) {
        Assert.notNull(episode, "episode must not be null");
        Long episodeId = episode.getId();
        if (episodeId != null && episodeId > 0) {
            return episodeRepository.findById(episodeId)
                .flatMap(entity -> copyProperties(episode, entity))
                .flatMap(episodeRepository::save)
                .flatMap(e -> copyProperties(e, episode));
        } else {
            return copyProperties(episode, new EpisodeEntity())
                .flatMap(episodeRepository::save)
                .flatMap(e -> copyProperties(e, episode));
        }
    }

    @Override
    @Cacheable(value = "episodeWithId", key = "#episodeId")
    public Mono<Episode> findById(Long episodeId) {
        Assert.isTrue(episodeId != null && episodeId > 0, "episode id must >= 0.");
        return episodeRepository.findById(episodeId)
            .flatMap(episodeEntity -> copyProperties(episodeEntity, new Episode()));
    }

    @Override
    @Cacheable(value = "episodesWithSubjectId", key = "#subjectId")
    public Flux<Episode> findAllBySubjectId(Long subjectId) {
        Assert.isTrue(subjectId >= 0, "'subjectId' must >= 0.");
        return episodeRepository.findAllBySubjectId(subjectId)
            .flatMap(episodeEntity -> copyProperties(episodeEntity, new Episode()));
    }

    @Override
    @Cacheable(value = "episodeWithSubjectIdAndGroupAndSeqAndName",
        key = "#subjectId + '-' + #group + '-' + #name")
    public Mono<Episode> findBySubjectIdAndGroupAndSequenceAndName(
        Long subjectId, EpisodeGroup group, Float sequence, String name) {
        Assert.isTrue(subjectId >= 0, "'subjectId' must >= 0.");
        Assert.notNull(group, "'group' must not be null");
        Assert.isTrue(sequence >= 0, "'sequence' must >= 0.");
        Assert.hasText(name, "'name' must not be empty.");
        return episodeRepository.findBySubjectIdAndGroupAndSequenceAndName(
                subjectId, group, sequence, name)
            .flatMap(episodeEntity -> copyProperties(episodeEntity, new Episode()));
    }

    @Override
    @Cacheable(value = "episodesWithSubjectIdAndGroupAndSeq",
        key = "#subjectId + '-' + #group + '-' + #sequence")
    public Flux<Episode> findBySubjectIdAndGroupAndSequence(Long subjectId, EpisodeGroup group,
                                                            Float sequence) {
        Assert.isTrue(subjectId >= 0, "'subjectId' must >= 0.");
        Assert.notNull(group, "'group' must not be null");
        Assert.isTrue(sequence >= 0, "'sequence' must >= 0.");
        return episodeRepository.findBySubjectIdAndGroupAndSequence(
            subjectId, group, sequence
        ).flatMap(episodeEntity -> copyProperties(episodeEntity, new Episode()));
    }

    @Override
    @CacheEvict(value = "episodeWithId", key = "#episodeId")
    public Mono<Void> deleteById(Long episodeId) {
        Assert.isTrue(episodeId >= 0, "'episodeId' must >= 0.");
        return episodeRepository.findById(episodeId)
            .flatMap(entity -> episodeRepository.delete(entity)
                .doOnSuccess(v -> {
                    log.debug("Remove exists episode: {}", entity);
                    EpisodeRemoveEvent event = new EpisodeRemoveEvent(this, entity);
                    applicationEventPublisher.publishEvent(event);
                }));
    }

    @Override
    @Cacheable(value = "episodeCountWithSubjectId", key = "#subjectId")
    public Mono<Long> countBySubjectId(Long subjectId) {
        Assert.isTrue(subjectId >= 0, "'subjectId' must >= 0.");
        return episodeRepository.countBySubjectId(subjectId);
    }

    @Override
    @Cacheable(value = "episodeMatchingCountWithSubjectId", key = "#subjectId")
    public Mono<Long> countMatchingBySubjectId(Long subjectId) {
        Assert.isTrue(subjectId >= 0, "'subjectId' must >= 0.");
        return databaseClient.sql("select count(e.ID) from EPISODE e, ATTACHMENT_REFERENCE ar "
                + "where ar.TYPE = 'EPISODE' and e.ID = ar.REFERENCE_ID "
                + "and e.SUBJECT_ID = :subjectId")
            .bind("subjectId", subjectId)
            .map(row -> row.get(0, Long.class))
            .one();
    }


    @Override
    @Cacheable(value = "episodesWithId", key = "#episodeId")
    public Flux<EpisodeResource> findResourcesById(Long episodeId) {
        Assert.isTrue(episodeId >= 0, "'episodeId' must >= 0.");
        return attachmentReferenceRepository
            .findAllByTypeAndReferenceIdOrderByTypeAscAttachmentIdAsc(
                AttachmentReferenceType.EPISODE, episodeId)
            .flatMap(attachmentReferenceEntity ->
                attachmentRepository.findById(attachmentReferenceEntity.getAttachmentId())
                    .map(attachmentEntity -> EpisodeResource.builder()
                        .episodeId(episodeId)
                        .attachmentId(attachmentEntity.getId())
                        .parentAttachmentId(attachmentEntity.getParentId())
                        .name(attachmentEntity.getName())
                        .url(attachmentEntity.getUrl())
                        .canRead(true)
                        .build())
            );
    }

    @Override
    @CacheEvict(value = {"episodeWithId", "episodesWithSubjectId",
        "episodeWithSubjectIdAndGroupAndSeqAndName",
        "episodesWithSubjectIdAndGroupAndSeq",
        "episodeCountWithSubjectId", "episodeMatchingCountWithSubjectId",
        "episodesWithId"
    })
    public Flux<Episode> updateEpisodesWithSubjectId(Long subjectId, List<Episode> episodes) {
        Assert.isTrue(subjectId >= 0, "'subjectId' must >= 0.");
        Assert.notNull(episodes, "'episodes' must not be null.");

        episodes.forEach(episode -> episode.setSubjectId(subjectId));

        // 移除新的列表里不存在的过期剧集
        //        return episodeRepository.findAllBySubjectId(subjectId)
        //            .filter(entity -> {
        //                Optional<Episode> episodeOptional = episodes.stream()
        //                  .filter(episode -> episode.getSubjectId().equals(entity.getSubjectId())
        //                        && episode.getSequence().equals(entity.getSequence())
        //                        && episode.getGroup().equals(entity.getGroup())
        //                    ).findFirst();
        //                return episodeOptional.isEmpty();
        //            })
        //            .flatMap(entity -> episodeRepository.delete(entity)
        //                .doOnSuccess(v -> {
        //                    log.debug("Remove exists episode: {}", entity);
        //                    EpisodeRemoveEvent event = new EpisodeRemoveEvent(this, entity);
        //                    applicationEventPublisher.publishEvent(event);
        //                }))
        //            // 更新或新增剧集
        //            .thenMany(Flux.fromIterable(episodes))
        return Flux.fromIterable(episodes)
            .flatMap(episode -> episodeRepository.findBySubjectIdAndGroupAndSequence(
                    subjectId, episode.getGroup(), episode.getSequence())
                .collectList().filter(entities -> !entities.isEmpty())
                .map(entities -> entities.get(0))
                .flatMap(entity -> copyProperties(episode, entity, "id"))
                .switchIfEmpty(copyProperties(episode, new EpisodeEntity())) // 如果没找到，则新增
            )
            .flatMap(episodeRepository::save)
            .flatMap(entity -> copyProperties(entity, new Episode()));
    }
}
