package run.ikaros.server.store.repository;

import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.RoleEntity;

public interface RoleRepository extends BaseRepository<RoleEntity> {
    Mono<RoleEntity> findByName(String name);

    Mono<Boolean> existsByName(String name);
}
