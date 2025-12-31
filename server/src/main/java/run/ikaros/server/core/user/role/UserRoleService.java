package run.ikaros.server.core.user.role;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.role.Role;
import run.ikaros.server.store.entity.UserRoleEntity;

public interface UserRoleService {

    Mono<UserRoleEntity> saveEntity(UserRoleEntity entity);

    Flux<Role> addUserRoles(UUID userId, UUID[] roleIds);

    Mono<Void> deleteUserRoles(UUID userId, UUID[] roleIds);

    Flux<Role> getRolesForUser(UUID userId);
}
