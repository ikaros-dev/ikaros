package run.ikaros.server.store.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.AuthorityType;
import run.ikaros.server.store.entity.AuthorityEntity;

public interface AuthorityRepository extends R2dbcRepository<AuthorityEntity, UUID> {
    Mono<AuthorityEntity> findByTypeAndTargetAndAuthority(
        AuthorityType type, String target, String authority
    );

    Mono<AuthorityEntity> findByAllowAndTypeAndTargetAndAuthority(
        boolean allow, AuthorityType type, String target, String authority
    );

    Flux<AuthorityEntity> findAllByType(AuthorityType type);
}
