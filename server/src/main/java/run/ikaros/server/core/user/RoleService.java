package run.ikaros.server.core.user;

import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.RoleEntity;

public interface RoleService {
    Mono<RoleEntity> findById(Long roleId);

    Mono<String> findNameById(Long roleId);

    Mono<RoleEntity> createIfNotExist(String role);
}
