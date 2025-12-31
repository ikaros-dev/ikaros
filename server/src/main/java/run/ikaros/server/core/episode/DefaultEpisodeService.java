package run.ikaros.server.core.episode;

import static run.ikaros.api.infra.utils.ReactiveBeanUtils.copyProperties;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.EpisodeRecord;
import run.ikaros.api.core.subject.EpisodeResource;
import run.ikaros.api.infra.utils.ReflectUtils;
import run.ikaros.api.store.enums.EpisodeGroup;
import run.ikaros.server.cache.annotation.FluxCacheEvict;
import run.ikaros.server.cache.annotation.FluxCacheable;
import run.ikaros.server.cache.annotation.MonoCacheEvict;
import run.ikaros.server.cache.annotation.MonoCacheable;
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
    @MonoCacheEvict
    public Mono<Episode> save(Episode episode) {
        Assert.notNull(episode, "episode must not be null");
        UUID episodeId = episode.getId();
        if (episodeId != null) {
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
    @MonoCacheable(value = "episode:id:", key = "#episodeId")
    public Mono<Episode> findById(UUID episodeId) {
        Assert.notNull(episodeId, "episode must not null.");
        return episodeRepository.findById(episodeId)
            .flatMap(episodeEntity -> copyProperties(episodeEntity, new Episode()));
    }

    @Override
    @FluxCacheable(value = "episodes:subjectId:", key = "#subjectId")
    public Flux<Episode> findAllBySubjectId(UUID subjectId) {
        Assert.notNull(subjectId, "subjectId must not null.");
        return episodeRepository.findAllBySubjectId(subjectId)
            .flatMap(episodeEntity -> copyProperties(episodeEntity, new Episode()));
    }

    @Override
    @FluxCacheable(value = "episodeRecords:subjectId:", key = "#subjectId")
    public Flux<EpisodeRecord> findRecordsBySubjectId(UUID subjectId) {
        Assert.notNull(subjectId, "subjectId must not null.");
        return findAllBySubjectId(subjectId)
            .flatMap(episode -> findResourcesById(episode.getId())
                .collectList()
                .flatMap(resources -> Mono.just(new EpisodeRecord(episode, resources)))
            );
    }

    @Override
    @MonoCacheable(value = "episode:subjectId_group_sequence_name",
        key = "#subjectId + #group + #sequence + #name")
    public Mono<Episode> findBySubjectIdAndGroupAndSequenceAndName(
        UUID subjectId, EpisodeGroup group, Float sequence, String name) {
        Assert.notNull(subjectId, "subjectId must not null.");
        Assert.notNull(group, "'group' must not be null");
        Assert.isTrue(sequence >= 0, "'sequence' must >= 0.");
        Assert.hasText(name, "'name' must not be empty.");
        return episodeRepository.findBySubjectIdAndGroupAndSequenceAndName(
                subjectId, group, sequence, name)
            .flatMap(episodeEntity -> copyProperties(episodeEntity, new Episode()));
    }

    @Override
    @FluxCacheable(value = "episode:subjectId_group_sequence",
        key = "#subjectId + #group + #sequence")
    public Flux<Episode> findBySubjectIdAndGroupAndSequence(UUID subjectId, EpisodeGroup group,
                                                            Float sequence) {
        Assert.notNull(subjectId, "subjectId must not null.");
        Assert.notNull(group, "'group' must not be null");
        Assert.isTrue(sequence >= 0, "'sequence' must >= 0.");
        return episodeRepository.findBySubjectIdAndGroupAndSequence(
            subjectId, group, sequence
        ).flatMap(episodeEntity -> copyProperties(episodeEntity, new Episode()));
    }

    @Override
    @MonoCacheEvict
    public Mono<Void> deleteById(UUID episodeId) {
        Assert.notNull(episodeId, "episodeId must not null.");
        return episodeRepository.findById(episodeId)
            .flatMap(entity -> episodeRepository.delete(entity)
                .doOnSuccess(v -> {
                    log.debug("Remove exists episode: {}", entity);
                    EpisodeRemoveEvent event = new EpisodeRemoveEvent(this, entity);
                    applicationEventPublisher.publishEvent(event);
                }));
    }

    @Override
    @MonoCacheable(value = "episode:count:subjectId", key = "#subjectId")
    public Mono<Long> countBySubjectId(UUID subjectId) {
        Assert.notNull(subjectId, "subjectId must not null.");
        return episodeRepository.countBySubjectId(subjectId);
    }

    @Override
    @MonoCacheable(value = "episode:countMatching:subjectId", key = "#subjectId")
    public Mono<Long> countMatchingBySubjectId(UUID subjectId) {
        Assert.notNull(subjectId, "'subjectId' must not null.");
        return databaseClient.sql("select count(e.ID) from EPISODE e, ATTACHMENT_REFERENCE ar "
                + "where ar.TYPE = 'EPISODE' and e.ID = ar.REFERENCE_ID "
                + "and e.SUBJECT_ID = :subjectId")
            .bind("subjectId", subjectId)
            .map(row -> row.get(0, Long.class))
            .one();
    }


    @Override
    @FluxCacheable(value = "episode_resources:episodeId", key = "#episodeId")
    public Flux<EpisodeResource> findResourcesById(UUID episodeId) {
        Assert.notNull(episodeId, "'episodeId' must not null.");
        return databaseClient.sql("select att_ref.ATTACHMENT_ID as attachment_id, "
                + "att.PARENT_ID as parent_attachment_id, "
                + "att_ref.REFERENCE_ID as episode_id, "
                + "att.URL as url, "
                + "att.NAME as name "
                + "from ATTACHMENT_REFERENCE att_ref, ATTACHMENT att "
                + "where att_ref.TYPE = 'EPISODE' and att_ref.REFERENCE_ID = :episodeId "
                + "and att_ref.ATTACHMENT_ID = att.ID "
                + "order by att_ref.TYPE, att_ref.ATTACHMENT_ID")
            .bind("episodeId", episodeId)
            .fetch()
            .all()
            .map(row -> ReflectUtils.mapToClass(row, EpisodeResource.class, true));
    }

    @Override
    @FluxCacheEvict
    public Flux<Episode> updateEpisodesWithSubjectId(UUID subjectId, List<Episode> episodes) {
        Assert.notNull(subjectId, "'subjectId' must not null.");
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
