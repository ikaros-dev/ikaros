package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.AuthorityType;
import run.ikaros.server.store.entity.AuthorityEntity;

public interface AuthorityRepository extends R2dbcRepository<AuthorityEntity, Long> {
    Mono<AuthorityEntity> findByTypeAndTargetAndAuthority(
        AuthorityType type, String target, String authority
    );

    Mono<AuthorityEntity> findByAllowAndTypeAndTargetAndAuthority(
        boolean allow, AuthorityType type, String target, String authority
    );
}
