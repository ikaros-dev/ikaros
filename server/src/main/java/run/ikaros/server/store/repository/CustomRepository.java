package run.ikaros.server.store.repository;

import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.CustomEntity;

/**
 * CustomEntity repository.
 *
 * @author: li-guohao
 * @see CustomEntity
 */
public interface CustomRepository extends BaseRepository<CustomEntity> {
    Flux<CustomEntity> findAllByGroupAndVersionAndKind(String group, String version, String kind,
                                                       Pageable pageable);

    Flux<CustomEntity> findAllByGroupAndVersionAndKind(String group, String version, String kind);

    Mono<Long> countCustomEntitiesByGroupAndVersionAndKind(String group, String version,
                                                           String kind);


    Mono<CustomEntity> findByGroupAndVersionAndKindAndName(String group, String version,
                                                           String kind, String name);
}
