package run.ikaros.server.core.user;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.role.Role;
import run.ikaros.server.store.entity.UserRoleEntity;

public interface UserRoleService {

    Mono<UserRoleEntity> saveEntity(UserRoleEntity entity);

    Flux<Role> addUserRoles(Long userId, Long[] roleIds);

    Mono<Void> deleteUserRoles(Long userId, Long[] roleIds);

    Flux<Role> getRolesForUser(Long userId);
}
