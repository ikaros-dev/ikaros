package run.ikaros.server.core.attachment.service.impl;

import static run.ikaros.api.infra.utils.ReactiveBeanUtils.copyProperties;
import static run.ikaros.api.store.enums.AttachmentReferenceType.EPISODE;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.AttachmentReference;
import run.ikaros.api.core.attachment.exception.AttachmentNotFoundException;
import run.ikaros.api.core.attachment.exception.AttachmentRefMatchingException;
import run.ikaros.api.infra.exception.RegexMatchingException;
import run.ikaros.api.infra.exception.subject.EpisodeNotFoundException;
import run.ikaros.api.infra.utils.RegexUtils;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.api.store.enums.EpisodeGroup;
import run.ikaros.server.core.attachment.event.AttachmentReferenceSaveEvent;
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
        return checkAttachmentRef(attachmentReference)
            .flatMap(attachmentRef ->
                copyProperties(attachmentRef, new AttachmentReferenceEntity()))
            .flatMap(attachmentReferenceEntity -> repository.save(attachmentReferenceEntity)
                .doOnSuccess(entity -> {
                    AttachmentReferenceSaveEvent event =
                        new AttachmentReferenceSaveEvent(this, entity);
                    applicationEventPublisher.publishEvent(event);
                    log.debug("publish AttachmentReferenceSaveEvent "
                        + "for attachment reference entity: [{}]", entity);
                }))
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
    public Mono<Void> removeAllByTypeAndReferenceId(AttachmentReferenceType type,
                                                    Long referenceId) {
        Assert.notNull(type, "'type' must not null.");
        Assert.isTrue(referenceId > 0, "'referenceId' must gt 0.");
        return repository.deleteAllByTypeAndReferenceId(type, referenceId);
    }

    @Override
    public Mono<Void> removeByTypeAndAttachmentIdAndReferenceId(AttachmentReferenceType type,
                                                                Long attachmentId,
                                                                Long referenceId) {
        Assert.notNull(type, "'type' must not null.");
        Assert.isTrue(attachmentId > 0, "'attachmentId' must gt 0.");
        Assert.isTrue(referenceId > 0, "'referenceId' must gt 0.");
        return repository.deleteByTypeAndAttachmentIdAndReferenceId(
            type, attachmentId, referenceId);
    }

    @Override
    public Mono<Void> matchingAttachmentsAndSubjectEpisodes(Long subjectId, Long[] attachmentIds) {
        return matchingAttachmentsAndSubjectEpisodes(subjectId, attachmentIds,
            EpisodeGroup.MAIN, false);
    }

    @Override
    public Mono<Void> matchingAttachmentsAndSubjectEpisodes(Long subjectId, Long[] attachmentIds,
                                                            EpisodeGroup group) {
        return matchingAttachmentsAndSubjectEpisodes(subjectId, attachmentIds,
            group, false);
    }

    @Override
    public Mono<Void> matchingAttachmentsAndSubjectEpisodes(Long subjectId, Long[] attachmentIds,
                                                            boolean notify) {
        Assert.isTrue(subjectId > 0, "'subjectId' must gt 0.");
        Assert.notNull(attachmentIds, "'attachmentIds' must not null.");
        return matchingAttachmentsAndSubjectEpisodes(subjectId, attachmentIds,
            EpisodeGroup.MAIN, notify);
    }

    @Override
    public Mono<Void> matchingAttachmentsAndSubjectEpisodes(Long subjectId, Long[] attachmentIds,
                                                            EpisodeGroup group, boolean notify) {
        Assert.isTrue(subjectId > 0, "'subjectId' must gt 0.");
        Assert.notNull(attachmentIds, "'attachmentIds' must not null.");
        return Flux.fromArray(attachmentIds)
            .flatMap(attId -> attachmentRepository.findById(attId)
                .switchIfEmpty(Mono.error(new AttachmentNotFoundException(
                    "Check fail, current attachment not found for id=" + attId))))
            .flatMap(entity -> getSeqMono(entity.getName())
                .flatMap(seq -> episodeRepository.findBySubjectIdAndGroupAndSequence(subjectId,
                        group == null ? EpisodeGroup.MAIN : group, seq)
                    .switchIfEmpty(Mono.error(new AttachmentRefMatchingException(
                        "Matching fail, episode not fond by seq=" + seq
                            + " and subjectId=" + subjectId
                            + " and ep group=" + group)))
                    .collectList().map(episodeEntities -> episodeEntities.get(0)))
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

    @Override
    public Mono<Void> matchingAttachmentsForEpisode(Long episodeId, Long[] attachmentIds) {
        Assert.isTrue(episodeId > 0, "'episodeId' must gt 0.");
        Assert.notNull(attachmentIds, "'attachmentIds' must not null.");
        // check episode exists
        return episodeRepository.existsById(episodeId)
            .filter(exists -> exists)
            .switchIfEmpty(
                Mono.error(new EpisodeNotFoundException("Episode not found for id=" + episodeId)))
            // check all attachments exists
            .flatMapMany(exists -> Flux.fromArray(attachmentIds))
            .flatMap(attId -> attachmentRepository.existsById(attId)
                .filter(exists -> exists)
                .switchIfEmpty(Mono.error(
                    new AttachmentNotFoundException("Attachment not found for id=" + attId))))
            .collectList()
            // save attachment ref records.
            .flatMapMany(attExistsList -> Flux.fromArray(attachmentIds))
            .map(attId -> AttachmentReferenceEntity.builder()
                .type(EPISODE).attachmentId(attId).referenceId(episodeId)
                .build())
            .flatMap(attachmentReferenceEntity -> repository.save(attachmentReferenceEntity)
                .doOnSuccess(entity -> {
                    log.info("save episode file matching "
                            + "for attId:[{}] and episodeId:[{}]. ",
                        entity.getAttachmentId(), entity.getReferenceId());
                    EpisodeAttachmentUpdateEvent event =
                        new EpisodeAttachmentUpdateEvent(this,
                            entity.getReferenceId(),
                            entity.getAttachmentId(), false);
                    applicationEventPublisher.publishEvent(event);
                    log.debug("publish event EpisodeAttachmentUpdateEvent "
                        + "for attachmentReferenceEntity: {}", attachmentReferenceEntity);
                }))
            .then();
    }

    private static Mono<Float> getSeqMono(String name) {
        Float seq;
        try {
            seq = Float.parseFloat(String.valueOf(RegexUtils.parseEpisodeSeqByFileName(name)));
            if (-1 == seq) {
                throw new RegexMatchingException("Matching fail");
            }
        } catch (RegexMatchingException regexMatchingException) {
            log.warn("parse episode seq by file name fail", regexMatchingException);
            return Mono.error(new AttachmentRefMatchingException(
                "Matching fail, current attachment name seq illegal, "
                    + "please rename you attachment name such as '[0x]' or ' 0x '",
                regexMatchingException));
        }
        return Mono.justOrEmpty(seq);
    }


    private Mono<AttachmentReference> checkAttachmentRef(AttachmentReference attachmentReference) {
        if (EPISODE.equals(attachmentReference.getType())) {
            return Mono.just(attachmentReference.getAttachmentId())
                .flatMap(attId -> attachmentRepository.findById(attId)
                    .switchIfEmpty(Mono.error(new AttachmentNotFoundException(
                        "Check fail, current attachment not found for id=" + attId))))
                .map(attachmentEntity -> attachmentReference);
        } else {
            return Mono.just(attachmentReference);
        }
    }
}
