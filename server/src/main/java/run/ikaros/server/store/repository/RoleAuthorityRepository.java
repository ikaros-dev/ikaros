package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import run.ikaros.server.store.entity.RoleAuthorityEntity;

public interface RoleAuthorityRepository extends R2dbcRepository<RoleAuthorityEntity, Long> {
    Flux<RoleAuthorityEntity> findByRoleId(Long roleId);
}
