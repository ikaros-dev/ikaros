package run.ikaros.server.core.attachment.service;

import reactor.core.publisher.Flux;
import run.ikaros.api.core.attachment.AttachmentRelation;
import run.ikaros.api.store.enums.AttachmentRelationType;

public interface AttachmentRelationService {
    Flux<AttachmentRelation> findAllByTypeAndAttachmentId(AttachmentRelationType type,
                                                          Long attachmentId);

}
