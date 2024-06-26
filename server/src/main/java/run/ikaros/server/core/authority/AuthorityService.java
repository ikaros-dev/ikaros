package run.ikaros.server.core.authority;

import reactor.core.publisher.Flux;
import run.ikaros.api.security.Authority;
import run.ikaros.api.store.enums.AuthorityType;

public interface AuthorityService {
    Flux<String> getAuthorityTypes();

    Flux<Authority> getAuthoritiesByType(AuthorityType type);
}
