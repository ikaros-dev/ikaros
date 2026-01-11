package run.ikaros.server.core.authority;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.authority.Authority;
import run.ikaros.api.core.authority.AuthorityCondition;
import run.ikaros.api.store.enums.AuthorityType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.store.entity.AuthorityEntity;

public interface AuthorityService {
    Flux<String> getAuthorityTypes();

    Flux<Authority> getAuthoritiesByType(AuthorityType type);

    Mono<AuthorityEntity> saveEntity(AuthorityEntity entity);

    Mono<Authority> save(Authority authority);

    Mono<Void> deleteById(UUID id);

    Mono<PagingWrap<Authority>> findAllByCondition(AuthorityCondition authorityCondition);
}
