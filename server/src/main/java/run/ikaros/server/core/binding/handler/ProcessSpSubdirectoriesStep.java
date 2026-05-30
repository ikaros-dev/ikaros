package run.ikaros.server.core.binding.handler;

import static run.ikaros.api.infra.utils.ReactiveBeanUtils.copyProperties;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.binding.DirectoryBindingContext;
import run.ikaros.api.core.binding.DirectoryBindingStep;
import run.ikaros.api.core.episode.EpisodeSequenceRegularResult;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.store.enums.EpisodeGroup;
import run.ikaros.server.core.episode.EpisodeService;
import run.ikaros.server.core.episode.sequence.EpisodeSequenceRegularService;
import run.ikaros.server.store.entity.AttachmentReferenceEntity;
import run.ikaros.server.store.repository.AttachmentReferenceRepository;
import run.ikaros.server.store.repository.AttachmentRepository;

/**
 * Step 8: Process SP subdirectories (OP, ED, SP, etc.)
 * Maps directory names to episode groups and binds child files.
 * Order: 75
 */
@Slf4j
@Component
public class ProcessSpSubdirectoriesStep implements DirectoryBindingStep {

    private final AttachmentRepository attachmentRepository;
    private final EpisodeService episodeService;
    private final AttachmentReferenceRepository attachmentReferenceRepository;
    private final EpisodeSequenceRegularService episodeSequenceRegularService;

    public ProcessSpSubdirectoriesStep(AttachmentRepository attachmentRepository,
                                       EpisodeService episodeService,
                                       AttachmentReferenceRepository referenceRepository,
                                       EpisodeSequenceRegularService
                                           episodeSequenceRegularService) {
        this.attachmentRepository = attachmentRepository;
        this.episodeService = episodeService;
        this.attachmentReferenceRepository = referenceRepository;
        this.episodeSequenceRegularService = episodeSequenceRegularService;
    }

    @Override
    public String name() {
        return "ProcessSpSubdirectories";
    }

    @Override
    public int order() {
        return 75;
    }

    @Override
    public boolean shouldSkip(DirectoryBindingContext context) {
        return context.getSpSubdirectoryAttachments() == null
            || context.getSpSubdirectoryAttachments().isEmpty()
            || context.getSubjectId() == null;
    }

    @Override
    public Mono<DirectoryBindingContext> execute(DirectoryBindingContext context) {
        UUID subjectId = context.getSubjectId();
        return Flux.fromIterable(context.getSpSubdirectoryAttachments())
            .concatMap(spDir -> processSpDirectory(spDir, subjectId, context))
            .then(Mono.just(context));
    }

    private Mono<Void> processSpDirectory(Attachment spDir, UUID subjectId,
                                          DirectoryBindingContext context) {
        return attachmentRepository.findAllByParentId(spDir.getId())
            .filter(att -> att.getType() == AttachmentType.File
                || att.getType() == AttachmentType.Driver_File)
            .concatMap(entity -> copyProperties(entity, Attachment.builder().build()))
            .concatMap(file -> createSpEpisodeAndBind(file, subjectId, context))
            .then();
    }

    private Mono<Void> createSpEpisodeAndBind(Attachment file, UUID subjectId,
                                              DirectoryBindingContext context) {
        return episodeSequenceRegularService.match(file.getName())
            .filter(obj -> true)
            .filter(EpisodeSequenceRegularResult::isMatched)
            .flatMap(res ->
                doCreateSpEpisode(file, subjectId, res.getEpGroup(), res.getSequence(), context));
    }

    private Mono<Void> doCreateSpEpisode(Attachment file, UUID subjectId,
                                         EpisodeGroup group,
                                         float seq,
                                         DirectoryBindingContext context) {
        Episode newEpisode = Episode.builder()
            .subjectId(subjectId)
            .name(cleanFileName(file.getName()))
            .sequence(seq)
            .group(group)
            .build();

        return episodeService.save(newEpisode)
            .doOnNext(saved -> {
                context.getCreatedEpisodes().add(saved);
                log.info("Created SP episode: name={}, group={}, seq={}",
                    saved.getName(), saved.getGroup(), saved.getSequence());
            })
            .flatMap(saved -> {
                AttachmentReferenceEntity ref = AttachmentReferenceEntity.builder()
                    .id(UuidV7Utils.generateUuid())
                    .type(AttachmentReferenceType.EPISODE)
                    .attachmentId(file.getId())
                    .referenceId(saved.getId())
                    .build();
                return attachmentReferenceRepository.insert(ref)
                    .doOnNext(savedRef -> {
                        context.getCreatedAttachmentRefs().add(
                            run.ikaros.api.core.attachment.AttachmentReference.builder()
                                .id(savedRef.getId())
                                .type(savedRef.getType())
                                .attachmentId(savedRef.getAttachmentId())
                                .referenceId(savedRef.getReferenceId())
                                .build()
                        );
                    })
                    .then();
            });
    }


    private String cleanFileName(String name) {
        if (name == null) {
            return "Unknown";
        }
        int dotIndex = name.lastIndexOf('.');
        String baseName = dotIndex > 0 ? name.substring(0, dotIndex) : name;
        Matcher matcher = Pattern.compile("\\[([^\\[\\]]+)\\]").matcher(name);
        while (matcher.find()) {
            baseName = baseName.replace(matcher.group(), "");
        }
        return baseName.trim();
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
