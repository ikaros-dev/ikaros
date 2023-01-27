package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.RoleEntity;

public interface RoleRepository extends R2dbcRepository<RoleEntity, Long> {
    Mono<RoleEntity> findByName(String name);

    Mono<Boolean> existsByName(String name);
}
