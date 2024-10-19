package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.TagType;
import run.ikaros.server.store.entity.TagEntity;

public interface TagRepository
    extends R2dbcRepository<TagEntity, Long> {

    Mono<TagEntity> findByTypeAndMasterIdAndName(TagType type, Long masterId, String name);

    Mono<Boolean> existsByTypeAndMasterIdAndName(TagType type, Long masterId, String name);
}
