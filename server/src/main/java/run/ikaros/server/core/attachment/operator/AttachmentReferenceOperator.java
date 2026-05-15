package run.ikaros.server.core.attachment.operator;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.AttachmentReference;
import run.ikaros.api.core.attachment.AttachmentReferenceOperate;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.server.core.attachment.service.AttachmentReferenceService;

@Slf4j
@Component
public class AttachmentReferenceOperator implements AttachmentReferenceOperate {
    private final AttachmentReferenceService service;

    public AttachmentReferenceOperator(AttachmentReferenceService service) {
        this.service = service;
    }

    @Override
    public Mono<AttachmentReference> save(AttachmentReference attachmentReference) {
        return service.save(attachmentReference);
    }

    @Override
    public Flux<AttachmentReference> findAllByTypeAndAttachmentId(AttachmentReferenceType type,
                                                                  UUID attachmentId) {
        return service.findAllByTypeAndAttachmentId(type, attachmentId);
    }

    @Override
    public Mono<Void> matchingAttachmentsAndSubjectEpisodes(UUID subjectId, UUID[] attachmentIds) {
        return service.matchingAttachmentsAndSubjectEpisodes(subjectId, attachmentIds);
    }

    @Override
    public Mono<Void> matchingAttachmentsAndSubjectEpisodes(UUID subjectId, UUID[] attachmentIds,
                                                            boolean notify) {
        return service.matchingAttachmentsAndSubjectEpisodes(subjectId, attachmentIds, notify);
    }

    @Override
    public Mono<Void> matchingAttachmentsForEpisode(UUID episodeId, UUID[] attachmentIds) {
        return service.matchingAttachmentsForEpisode(episodeId, attachmentIds);
    }
}
