package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.AttachmentDriverEntity;

public interface AttachmentDriverRepository
    extends R2dbcRepository<AttachmentDriverEntity, Long> {
    Mono<AttachmentDriverEntity> findByTypeAndName(String type, String name);

    Mono<Long> deleteByTypeAndName(String type, String name);
}
