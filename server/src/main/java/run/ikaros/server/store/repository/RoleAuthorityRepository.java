package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.RoleAuthorityEntity;

public interface RoleAuthorityRepository extends R2dbcRepository<RoleAuthorityEntity, Long> {
    Flux<RoleAuthorityEntity> findByRoleId(Long roleId);

    Mono<Boolean> deleteByRoleIdAndAuthorityId(Long roleId, Long authorityId);
}
