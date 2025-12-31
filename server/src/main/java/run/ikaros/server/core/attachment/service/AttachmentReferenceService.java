package run.ikaros.server.core.attachment.service;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.AttachmentReference;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.api.store.enums.EpisodeGroup;

public interface AttachmentReferenceService {
    Mono<AttachmentReference> save(AttachmentReference attachmentReference);

    Flux<AttachmentReference> findAllByTypeAndAttachmentId(
        AttachmentReferenceType type, UUID attachmentId);

    Mono<Void> removeById(UUID attachmentRefId);

    Mono<Void> removeAllByTypeAndReferenceId(AttachmentReferenceType type, UUID referenceId);

    Mono<Void> removeByTypeAndAttachmentIdAndReferenceId(
        AttachmentReferenceType type, UUID attachmentId, UUID referenceId);

    Mono<Void> matchingAttachmentsAndSubjectEpisodes(UUID subjectId, UUID[] attachmentIds);

    Mono<Void> matchingAttachmentsAndSubjectEpisodes(UUID subjectId, UUID[] attachmentIds,
                                                     EpisodeGroup group);

    Mono<Void> matchingAttachmentsAndSubjectEpisodes(UUID subjectId, UUID[] attachmentIds,
                                                     boolean notify);

    Mono<Void> matchingAttachmentsAndSubjectEpisodes(UUID subjectId, UUID[] attachmentIds,
                                                     EpisodeGroup group, boolean notify);

    Mono<Void> matchingAttachmentsForEpisode(UUID episodeId, UUID[] attachmentIds);

}
