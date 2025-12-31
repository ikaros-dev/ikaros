package run.ikaros.server.store.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.RoleEntity;

public interface RoleRepository extends R2dbcRepository<RoleEntity, UUID> {
    Mono<RoleEntity> findByName(String name);

    Mono<Boolean> existsByName(String name);
}
