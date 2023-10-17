package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.server.store.entity.AttachmentReferenceEntity;

public interface AttachmentReferenceRepository
    extends R2dbcRepository<AttachmentReferenceEntity, Long> {
    Flux<AttachmentReferenceEntity> findAllByTypeAndAttachmentId(
        AttachmentReferenceType type, Long attachmentId);
}
