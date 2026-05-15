package run.ikaros.api.core.attachment;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.plugin.AllowPluginOperate;
import run.ikaros.api.store.enums.AttachmentReferenceType;

public interface AttachmentReferenceOperate extends AllowPluginOperate {
    Mono<AttachmentReference> save(AttachmentReference attachmentReference);

    Flux<AttachmentReference> findAllByTypeAndAttachmentId(
        AttachmentReferenceType type, UUID attachmentId);

    Mono<Void> matchingAttachmentsAndSubjectEpisodes(UUID subjectId, UUID[] attachmentIds);

    Mono<Void> matchingAttachmentsAndSubjectEpisodes(UUID subjectId, UUID[] attachmentIds,
                                                     boolean notify);

    Mono<Void> matchingAttachmentsForEpisode(UUID episodeId, UUID[] attachmentIds);
}
