package run.ikaros.server.core.user;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.role.Role;

public interface UserRoleService {
    Flux<Role> addUserRoles(Long userId, Long[] roleIds);

    Mono<Void> deleteUserRoles(Long userId, Long[] roleIds);

    Flux<Role> getRolesForUser(Long userId);
}
