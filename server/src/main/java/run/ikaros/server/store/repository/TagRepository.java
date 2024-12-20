package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.TagType;
import run.ikaros.server.store.entity.TagEntity;

public interface TagRepository
    extends R2dbcRepository<TagEntity, Long> {

    Flux<TagEntity> findAllByTypeAndMasterId(TagType type, Long masterId);

    Mono<TagEntity> findByTypeAndMasterIdAndName(TagType type, Long masterId, String name);

    Mono<Boolean> existsByTypeAndMasterIdAndName(TagType type, Long masterId, String name);

    Mono<Boolean> existsByTypeAndMasterIdAndUserIdAndName(
        TagType type, Long masterId, Long userId, String name);

    Mono<Boolean> existsByTypeAndUserIdAndName(
        TagType type, Long userId, String name);
}
