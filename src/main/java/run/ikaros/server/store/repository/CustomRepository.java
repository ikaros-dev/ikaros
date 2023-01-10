package run.ikaros.server.store.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import run.ikaros.server.store.entity.CustomEntity;

/**
 * CustomEntity repository.
 *
 * @author: li-guohao
 * @see CustomEntity
 */
public interface CustomRepository extends R2dbcRepository<CustomEntity, Long> {
    Flux<CustomEntity> findAllByGroupAndVersionAndKind(String group, String version, String kind,
                                                       Pageable pageable);
}
