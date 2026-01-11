package run.ikaros.server.store.repository;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.RoleAuthorityEntity;

public interface RoleAuthorityRepository extends BaseRepository<RoleAuthorityEntity> {
    Flux<RoleAuthorityEntity> findByRoleId(UUID roleId);

    Mono<Boolean> deleteByRoleIdAndAuthorityId(UUID roleId, UUID authorityId);
}
