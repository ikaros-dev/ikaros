package run.ikaros.server.store.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.AttachmentRelationType;
import run.ikaros.server.store.entity.AttachmentRelationEntity;

public interface AttachmentRelationRepository
    extends R2dbcRepository<AttachmentRelationEntity, UUID> {
    Mono<AttachmentRelationEntity> findByTypeAndAttachmentIdAndRelationAttachmentId(
        AttachmentRelationType type, UUID attachmentId, UUID relationId);

    Flux<AttachmentRelationEntity> findAllByAttachmentId(UUID attachmentId);

    Flux<AttachmentRelationEntity> findAllByTypeAndAttachmentId(AttachmentRelationType type,
                                                                UUID attachmentId);

    Mono<Boolean> existsByAttachmentId(UUID attachmentId);

    Mono<Boolean> existsByTypeAndAttachmentIdAndRelationAttachmentId(AttachmentRelationType type,
                                                                     UUID attachmentId,
                                                                     UUID relationAttachmentId);

    Mono<Void> deleteByTypeAndAttachmentIdAndRelationAttachmentId(AttachmentRelationType type,
                                                                  UUID attachmentId,
                                                                  UUID relationAttachmentId);

    Mono<Void> deleteAllByAttachmentId(UUID attachmentId);
}
