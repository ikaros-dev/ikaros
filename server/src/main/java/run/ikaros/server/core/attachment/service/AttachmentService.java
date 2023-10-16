package run.ikaros.server.core.attachment.service;

import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentSearchCondition;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.store.entity.AttachmentEntity;

public interface AttachmentService {
    Mono<AttachmentEntity> saveEntity(AttachmentEntity attachmentEntity);

    Mono<Attachment> save(Attachment attachment);

    Mono<PagingWrap<AttachmentEntity>> listEntitiesByCondition(AttachmentSearchCondition condition);

    Mono<PagingWrap<Attachment>> listByCondition(AttachmentSearchCondition condition);
}
