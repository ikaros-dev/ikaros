package run.ikaros.server.store.repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.AttachmentDriverEntity;

public interface AttachmentDriverRepository
    extends BaseRepository<AttachmentDriverEntity> {
    Mono<AttachmentDriverEntity> findByTypeAndName(String type, String name);

    Mono<AttachmentDriverEntity> findByTypeAndNameAndMountName(
        String type, String name, String mountName);

    Mono<Long> deleteByTypeAndName(String type, String name);

    Flux<AttachmentDriverEntity> findAllByTypeAndEnable(String type, boolean enable);
}
