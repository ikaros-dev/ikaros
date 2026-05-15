package run.ikaros.server.store.repository;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.api.store.enums.AttachmentRelationType;
import run.ikaros.server.store.entity.AttachmentReferenceEntity;

public interface AttachmentReferenceRepository
    extends BaseRepository<AttachmentReferenceEntity> {
    Flux<AttachmentReferenceEntity> findAllByTypeAndAttachmentId(
        AttachmentReferenceType type, UUID attachmentId);

    Mono<Boolean> existsByTypeAndAttachmentId(
        AttachmentReferenceType type, UUID attachmentId);

    Mono<Boolean> existsByTypeAndReferenceId(
        AttachmentRelationType type, UUID referenceId
    );

    Mono<Boolean> existsByTypeAndReferenceId(AttachmentReferenceType type, UUID referenceId);

    Mono<Boolean> existsByAttachmentId(UUID attachmentId);

    Flux<AttachmentReferenceEntity> findAllByAttachmentId(UUID attachmentId);

    Mono<AttachmentReferenceEntity> findByTypeAndAttachmentIdAndReferenceId(
        AttachmentReferenceType type, UUID attachmentId, UUID referenceId
    );


    Flux<AttachmentReferenceEntity> findAllByTypeAndReferenceId(AttachmentReferenceType type,
                                                                UUID referenceId);

    Mono<Long> countByTypeAndReferenceId(AttachmentReferenceType type,
                                         UUID referenceId);

    Flux<AttachmentReferenceEntity> findAllByTypeAndReferenceIdOrderByTypeAscAttachmentIdAsc(
        AttachmentReferenceType type, UUID referenceId);

    Mono<Void> deleteByTypeAndAttachmentIdAndReferenceId(
        AttachmentReferenceType type, UUID attachmentId, UUID referenceId);

    Mono<Void> deleteAllByTypeAndReferenceId(AttachmentReferenceType type, UUID referenceId);

    Mono<Void> deleteAllByAttachmentId(UUID attachmentId);
}
