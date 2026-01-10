package run.ikaros.server.store.repository;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.TagType;
import run.ikaros.server.store.entity.TagEntity;

public interface TagRepository
    extends BaseRepository<TagEntity> {

    Flux<TagEntity> findAllByTypeAndMasterId(TagType type, UUID masterId);

    Flux<TagEntity> findAllByTypeAndUserIdAndName(TagType type, UUID userId, String name);

    Mono<TagEntity> findByTypeAndMasterIdAndName(TagType type, UUID masterId, String name);

    Mono<TagEntity> findByTypeAndMasterIdAndUserIdAndName(
        TagType type, UUID masterId, UUID userId, String name);

    Mono<Boolean> existsByTypeAndMasterIdAndName(TagType type, UUID masterId, String name);

    Mono<Boolean> existsByTypeAndMasterIdAndUserIdAndName(
        TagType type, UUID masterId, UUID userId, String name);

    Mono<Boolean> existsByTypeAndUserIdAndName(
        TagType type, UUID userId, String name);
}
