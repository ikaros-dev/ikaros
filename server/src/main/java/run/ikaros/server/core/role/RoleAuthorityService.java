package run.ikaros.server.core.role;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.authority.Authority;

public interface RoleAuthorityService {
    Flux<Authority> addAuthoritiesForRole(UUID roleId, UUID[] authorityIds);

    Mono<Void> deleteAuthoritiesForRole(UUID roleId, UUID[] authorityIds);

    Flux<Authority> getAuthoritiesForRole(UUID roleId);
}
