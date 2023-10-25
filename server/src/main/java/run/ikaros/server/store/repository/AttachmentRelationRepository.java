package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.AttachmentRelationType;
import run.ikaros.server.store.entity.AttachmentRelationEntity;

public interface AttachmentRelationRepository
    extends R2dbcRepository<AttachmentRelationEntity, Long> {
    Flux<AttachmentRelationEntity> findAllByTypeAndAttachmentId(AttachmentRelationType type,
                                                                Long attachmentId);

    Mono<Boolean> existsByTypeAndAttachmentIdAndRelationAttachmentId(AttachmentRelationType type,
                                                                     Long attachmentId,
                                                                     Long relationAttachmentId);

}
