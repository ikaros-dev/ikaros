package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.server.store.entity.AttachmentReferenceEntity;

public interface AttachmentReferenceRepository
    extends R2dbcRepository<AttachmentReferenceEntity, Long> {
    Flux<AttachmentReferenceEntity> findAllByTypeAndAttachmentId(
        AttachmentReferenceType type, Long attachmentId);

    Mono<Boolean> existsByTypeAndAttachmentId(
        AttachmentReferenceType type, Long attachmentId);

    Mono<Boolean> existsByAttachmentId(Long attachmentId);

    Mono<AttachmentReferenceEntity> findByTypeAndAttachmentIdAndReferenceId(
        AttachmentReferenceType type, Long attachmentId, Long referenceId
    );

    Mono<Boolean> existsByTypeAndReferenceId(AttachmentReferenceType type, Long referenceId);

    Flux<AttachmentReferenceEntity> findAllByTypeAndReferenceId(AttachmentReferenceType type,
                                                                Long referenceId);

    Flux<AttachmentReferenceEntity> findAllByTypeAndReferenceIdOrderByTypeAscAttachmentIdAsc(
        AttachmentReferenceType type, Long referenceId);

    Mono<Void> deleteByTypeAndAttachmentIdAndReferenceId(
        AttachmentReferenceType type, Long attachmentId, Long referenceId);

    Mono<Void> deleteAllByTypeAndReferenceId(AttachmentReferenceType type, Long referenceId);

}
