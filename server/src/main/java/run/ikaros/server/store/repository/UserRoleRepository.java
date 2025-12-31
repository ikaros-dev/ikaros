package run.ikaros.server.store.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.UserRoleEntity;

public interface UserRoleRepository extends R2dbcRepository<UserRoleEntity, UUID> {
    Flux<UserRoleEntity> findByUserId(UUID userId);

    Mono<UserRoleEntity> findByUserIdAndRoleId(UUID userId, UUID roleId);

    Mono<Void> deleteByUserIdAndRoleId(UUID userId, UUID roleId);
}
