package run.ikaros.server.core.authority;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.security.Authority;
import run.ikaros.api.store.enums.AuthorityType;
import run.ikaros.server.store.entity.AuthorityEntity;

public interface AuthorityService {
    Flux<String> getAuthorityTypes();

    Flux<Authority> getAuthoritiesByType(AuthorityType type);

    Mono<AuthorityEntity> saveEntity(AuthorityEntity entity);
}
