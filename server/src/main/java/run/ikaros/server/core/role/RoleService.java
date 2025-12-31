package run.ikaros.server.core.role;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.role.Role;

public interface RoleService {
    Mono<Role> findById(UUID roleId);

    Mono<String> findNameById(UUID roleId);

    Mono<Role> createIfNotExist(String role);

    Flux<Role> findAll();

    Mono<Void> deleteById(UUID roleId);

    Mono<Role> save(Role role);
}
