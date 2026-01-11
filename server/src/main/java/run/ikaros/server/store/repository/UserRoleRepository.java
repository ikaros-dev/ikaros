package run.ikaros.server.store.repository;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.UserRoleEntity;

public interface UserRoleRepository extends BaseRepository<UserRoleEntity> {
    Flux<UserRoleEntity> findByUserId(UUID userId);

    Mono<UserRoleEntity> findByUserIdAndRoleId(UUID userId, UUID roleId);

    Mono<Void> deleteByUserIdAndRoleId(UUID userId, UUID roleId);
}
