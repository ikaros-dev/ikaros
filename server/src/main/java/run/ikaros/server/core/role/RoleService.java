package run.ikaros.server.core.role;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.role.Role;

public interface RoleService {
    Mono<Role> findById(Long roleId);

    Mono<String> findNameById(Long roleId);

    Mono<Role> createIfNotExist(String role);

    Flux<Role> findAll();

    Mono<Void> deleteById(Long roleId);

    Mono<Role> save(Role role);
}
