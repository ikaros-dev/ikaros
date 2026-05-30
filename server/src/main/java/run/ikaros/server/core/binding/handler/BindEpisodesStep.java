package run.ikaros.server.core.binding.handler;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.binding.DirectoryBindingContext;
import run.ikaros.api.core.binding.DirectoryBindingStep;
import run.ikaros.api.core.episode.EpisodeSequenceRegularResult;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.server.core.episode.sequence.EpisodeSequenceRegularService;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.api.store.enums.EpisodeGroup;
import run.ikaros.server.core.attachment.event.EpisodeAttachmentUpdateEvent;
import run.ikaros.server.core.episode.EpisodeService;
import run.ikaros.server.store.entity.AttachmentReferenceEntity;
import run.ikaros.server.store.repository.AttachmentReferenceRepository;
import run.ikaros.server.store.repository.EpisodeRepository;

/**
 * Step 7: Match video files to episodes by sequence number.
 * Creates episodes if not found, then creates attachment references.
 * Order: 70
 */
@Slf4j
@Component
public class BindEpisodesStep implements DirectoryBindingStep {

    private final EpisodeRepository episodeRepository;
    private final EpisodeService episodeService;
    private final AttachmentReferenceRepository attachmentReferenceRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EpisodeSequenceRegularService episodeSequenceRegularService;

    public BindEpisodesStep(EpisodeRepository episodeRepository,
                            EpisodeService episodeService,
                            AttachmentReferenceRepository attachmentReferenceRepository,
                            ApplicationEventPublisher eventPublisher,
                            EpisodeSequenceRegularService episodeSequenceRegularService) {
        this.episodeRepository = episodeRepository;
        this.episodeService = episodeService;
        this.attachmentReferenceRepository = attachmentReferenceRepository;
        this.eventPublisher = eventPublisher;
        this.episodeSequenceRegularService = episodeSequenceRegularService;
    }

    @Override
    public String name() {
        return "BindEpisodes";
    }

    @Override
    public int order() {
        return 70;
    }

    @Override
    public boolean shouldSkip(DirectoryBindingContext context) {
        return context.getChildAttachments() == null || context.getChildAttachments().isEmpty()
            || context.getSubjectId() == null;
    }

    @Override
    public Mono<DirectoryBindingContext> execute(DirectoryBindingContext context) {
        UUID subjectId = context.getSubjectId();
        return Flux.fromIterable(context.getChildAttachments())
            .concatMap(attachment -> bindFileToEpisode(attachment, subjectId, context))
            .then(Mono.just(context));
    }

    private Mono<Void> bindFileToEpisode(Attachment attachment, UUID subjectId,
                                         DirectoryBindingContext context) {
        return episodeSequenceRegularService.match(attachment.getName())
            .flatMap(result -> {
                if (!result.isMatched() || result.getSequence() == null) {
                    log.warn("Cannot parse episode sequence from file: {}",
                        attachment.getName());
                    return Mono.<Void>empty();
                }
                float seq = result.getSequence();
                EpisodeGroup group = result.getEpGroup() != null
                    ? result.getEpGroup() : EpisodeGroup.MAIN;
                return doBindFile(attachment, subjectId, seq, group, context);
            });
    }

    private Mono<Void> doBindFile(Attachment attachment, UUID subjectId,
                                   float seq, EpisodeGroup group,
                                   DirectoryBindingContext context) {
        return episodeRepository.findBySubjectIdAndGroupAndSequence(
                subjectId, group, seq)
            .collectList()
            .filter(list -> !list.isEmpty())
            .map(list -> list.get(0))
            .flatMap(episodeEntity -> {
                Episode episode = Episode.builder()
                    .id(episodeEntity.getId())
                    .subjectId(episodeEntity.getSubjectId())
                    .name(episodeEntity.getName())
                    .sequence(episodeEntity.getSequence())
                    .group(episodeEntity.getGroup())
                    .build();
                return createRefAndRecord(attachment, episode, context);
            })
            .switchIfEmpty(createEpisodeAndBind(attachment, subjectId, seq, group, context));
    }

    private Mono<Void> createEpisodeAndBind(Attachment attachment, UUID subjectId,
                                            float seq, EpisodeGroup group,
                                            DirectoryBindingContext context) {
        Episode newEpisode = Episode.builder()
            .id(UuidV7Utils.generateUuid())
            .subjectId(subjectId)
            .name("Episode " + (int) seq)
            .sequence(seq)
            .group(group)
            .build();

        return episodeService.save(newEpisode)
            .doOnNext(saved -> {
                context.getCreatedEpisodes().add(saved);
                log.info("Created episode: name={}, seq={}, subjectId={}",
                    saved.getName(), saved.getSequence(), subjectId);
            })
            .flatMap(saved -> createRefAndRecord(attachment, saved, context));
    }

    private Mono<Void> createRefAndRecord(Attachment attachment, Episode episode,
                                          DirectoryBindingContext context) {
        AttachmentReferenceEntity ref = AttachmentReferenceEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .type(AttachmentReferenceType.EPISODE)
            .attachmentId(attachment.getId())
            .referenceId(episode.getId())
            .build();

        return attachmentReferenceRepository.insert(ref)
            .doOnNext(saved -> {
                context.getCreatedAttachmentRefs().add(
                    run.ikaros.api.core.attachment.AttachmentReference.builder()
                        .id(saved.getId())
                        .type(saved.getType())
                        .attachmentId(saved.getAttachmentId())
                        .referenceId(saved.getReferenceId())
                        .build()
                );
                log.info("Bound file [{}] to episode seq [{}]",
                    attachment.getName(), episode.getSequence());

                eventPublisher.publishEvent(new EpisodeAttachmentUpdateEvent(
                    this, episode.getId(), attachment.getId(), false));
                log.debug("Published EpisodeAttachmentUpdateEvent "
                        + "for episodeId={}, attachmentId={}",
                    episode.getId(), attachment.getId());
            })
            .then();
    }

    @Override
    public Mono<Void> rollback(DirectoryBindingContext context) {
        Mono<Void> removeRefs = Flux.fromIterable(context.getCreatedAttachmentRefs())
            .concatMap(ref -> attachmentReferenceRepository.deleteById(ref.getId())
                .onErrorResume(e -> {
                    log.warn("Failed to remove attachment ref during rollback", e);
                    return Mono.empty();
                }))
            .then();

        Mono<Void> removeEpisodes = Flux.fromIterable(context.getCreatedEpisodes())
            .concatMap(ep -> episodeService.deleteById(ep.getId())
                .onErrorResume(e -> {
                    log.warn("Failed to remove episode during rollback", e);
                    return Mono.empty();
                }))
            .then();

        return removeRefs.then(removeEpisodes);
    }
}
