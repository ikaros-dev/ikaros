package run.ikaros.server.store.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.RoleAuthorityEntity;

public interface RoleAuthorityRepository extends R2dbcRepository<RoleAuthorityEntity, UUID> {
    Flux<RoleAuthorityEntity> findByRoleId(UUID roleId);

    Mono<Boolean> deleteByRoleIdAndAuthorityId(UUID roleId, UUID authorityId);
}
