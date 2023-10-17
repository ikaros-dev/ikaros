package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.store.entity.AttachmentEntity;

public interface AttachmentRepository extends R2dbcRepository<AttachmentEntity, Long> {
    Mono<Boolean> existsByTypeAndParentIdAndName(AttachmentType type, Long parentId, String name);

    Mono<AttachmentEntity> findByTypeAndParentIdAndName(
        AttachmentType type, Long parentId, String name);

    Mono<Void> removeByTypeAndParentIdAndName(
        AttachmentType type, Long parentId, String name);
}
