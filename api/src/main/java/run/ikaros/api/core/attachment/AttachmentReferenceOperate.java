package run.ikaros.api.core.attachment;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.plugin.AllowPluginOperate;
import run.ikaros.api.store.enums.AttachmentReferenceType;

public interface AttachmentReferenceOperate extends AllowPluginOperate {
    Mono<AttachmentReference> save(AttachmentReference attachmentReference);

    Flux<AttachmentReference> findAllByTypeAndAttachmentId(
        AttachmentReferenceType type, Long attachmentId);

    Mono<Void> matchingAttachmentsAndSubjectEpisodes(Long subjectId, Long[] attachmentIds);

    Mono<Void> matchingAttachmentsAndSubjectEpisodes(Long subjectId, Long[] attachmentIds,
                                                     boolean notify);

    Mono<Void> matchingAttachmentsForEpisode(Long episodeId, Long[] attachmentIds);
}
