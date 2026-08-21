package run.ikaros.server.store.repository;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.store.entity.AttachmentEntity;

public interface AttachmentRepository extends BaseRepository<AttachmentEntity> {
    Mono<Boolean> existsByTypeAndParentIdAndName(AttachmentType type, UUID parentId, String name);

    Mono<Boolean> existsByParentIdAndName(UUID parentId, String name);

    Mono<Void> removeByTypeAndParentIdAndName(
        AttachmentType type, UUID parentId, String name);

    Mono<AttachmentEntity> findByTypeAndParentIdAndName(
        AttachmentType type, UUID parentId, String name);

    Flux<AttachmentEntity> findAllByParentId(UUID parentId);

    Flux<AttachmentEntity> findAllByTypeAndNameLike(AttachmentType type, String name);

    Mono<AttachmentEntity> findByUrl(String url);

    Mono<Long> countByType(AttachmentType type);
}
