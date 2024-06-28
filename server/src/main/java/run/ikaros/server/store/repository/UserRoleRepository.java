package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.UserRoleEntity;

public interface UserRoleRepository extends R2dbcRepository<UserRoleEntity, Long> {
    Flux<UserRoleEntity> findByUserId(Long userId);

    Mono<UserRoleEntity> findByUserIdAndRoleId(Long userId, Long roleId);

    Mono<Void> deleteByUserIdAndRoleId(Long userId, Long roleId);
}
