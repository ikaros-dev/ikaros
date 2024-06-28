package run.ikaros.server.core.role;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.authority.Authority;

public interface RoleAuthorityService {
    Flux<Authority> addAuthoritiesForRole(Long roleId, Long[] authorityIds);

    Mono<Void> deleteAuthoritiesForRole(Long roleId, Long[] authorityIds);

    Flux<Authority> getAuthoritiesForRole(Long roleId);
}
