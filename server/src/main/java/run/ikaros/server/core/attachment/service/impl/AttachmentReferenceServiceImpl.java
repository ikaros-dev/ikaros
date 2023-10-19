package run.ikaros.server.core.attachment.service.impl;

import static run.ikaros.api.store.enums.AttachmentReferenceType.EPISODE;
import static run.ikaros.server.infra.utils.ReactiveBeanUtils.copyProperties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.AttachmentReference;
import run.ikaros.api.infra.exception.RegexMatchingException;
import run.ikaros.api.infra.utils.FileUtils;
import run.ikaros.api.infra.utils.RegexUtils;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.api.store.enums.EpisodeGroup;
import run.ikaros.server.core.attachment.event.EpisodeAttachmentUpdateEvent;
import run.ikaros.server.core.attachment.service.AttachmentReferenceService;
import run.ikaros.server.store.entity.AttachmentReferenceEntity;
import run.ikaros.server.store.repository.AttachmentReferenceRepository;
import run.ikaros.server.store.repository.AttachmentRepository;
import run.ikaros.server.store.repository.EpisodeRepository;

@Slf4j
@Service
public class AttachmentReferenceServiceImpl implements AttachmentReferenceService {
    private final AttachmentReferenceRepository repository;
    private final AttachmentRepository attachmentRepository;
    private final EpisodeRepository episodeRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Construct.
     */
    public AttachmentReferenceServiceImpl(AttachmentReferenceRepository repository,
                                          AttachmentRepository attachmentRepository,
                                          EpisodeRepository episodeRepository,
                                          ApplicationEventPublisher applicationEventPublisher) {
        this.repository = repository;
        this.attachmentRepository = attachmentRepository;
        this.episodeRepository = episodeRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }


    @Override
    public Mono<AttachmentReference> save(AttachmentReference attachmentReference) {
        Assert.notNull(attachmentReference, "'attachmentReference' must not null.");
        return copyProperties(attachmentReference, new AttachmentReferenceEntity())
            .flatMap(repository::save)
            .flatMap(entity -> copyProperties(entity, attachmentReference));
    }

    @Override
    public Flux<AttachmentReference> findAllByTypeAndAttachmentId(AttachmentReferenceType type,
                                                                  Long attachmentId) {
        Assert.notNull(type, "'type' must not null.");
        Assert.isTrue(attachmentId > 0, "'attachmentId' must > 0.");
        return repository.findAllByTypeAndAttachmentId(type, attachmentId)
            .flatMap(entity -> copyProperties(entity, new AttachmentReference()));
    }

    @Override
    public Mono<Void> removeById(Long attachmentRefId) {
        return repository.deleteById(attachmentRefId);
    }

    @Override
    public Mono<Void> matchingAttachmentsAndSubjectEpisodes(Long subjectId, Long[] attachmentIds) {
        return matchingAttachmentsAndSubjectEpisodes(subjectId, attachmentIds, false);
    }

    @Override
    public Mono<Void> matchingAttachmentsAndSubjectEpisodes(Long subjectId, Long[] attachmentIds,
                                                            boolean notify) {
        Assert.isTrue(subjectId > 0, "'subjectId' must gt 0.");
        Assert.notNull(attachmentIds, "'attachmentIds' must not null.");
        return Flux.fromArray(attachmentIds)
            .flatMap(attachmentRepository::findById)
            .filter(entity -> FileUtils.isVideo(entity.getUrl()))
            .flatMap(entity -> getSeqMono(entity.getName())
                .flatMap(seq -> episodeRepository.findBySubjectIdAndGroupAndSequence(subjectId,
                    EpisodeGroup.MAIN, seq))
                .flatMap(episodeEntity -> repository
                    .existsByTypeAndReferenceId(EPISODE, episodeEntity.getId())
                    .filter(exists -> !exists)
                    .flatMap(exists -> repository
                        .save(AttachmentReferenceEntity.builder()
                            .type(EPISODE)
                            .attachmentId(entity.getId())
                            .referenceId(episodeEntity.getId())
                            .build())
                        .doOnSuccess(attachmentReferenceEntity -> {
                            log.info("save episode file matching "
                                    + "for attachment name:[{}] and episode seq:[{}] "
                                    + "when subjectId=[{}].",
                                entity.getName(), episodeEntity.getSequence(), subjectId);
                            EpisodeAttachmentUpdateEvent event =
                                new EpisodeAttachmentUpdateEvent(this,
                                    episodeEntity.getId(),
                                    entity.getId(), notify);
                            applicationEventPublisher.publishEvent(event);
                            log.debug("publish event EpisodeAttachmentUpdateEvent "
                                + "for attachmentReferenceEntity: {}", attachmentReferenceEntity);
                        }))
                ))
            .then();
    }

    private static Mono<Integer> getSeqMono(String name) {
        int seq;
        try {
            seq = Integer.parseInt(String.valueOf(RegexUtils.parseEpisodeSeqByFileName(name)));
        } catch (RegexMatchingException regexMatchingException) {
            log.warn("parse episode seq by file name fail", regexMatchingException);
            return Mono.empty();
        }
        return Mono.justOrEmpty(seq);
    }
}
