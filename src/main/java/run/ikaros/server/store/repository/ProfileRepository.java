package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.ProfileEntity;

public interface ProfileRepository extends R2dbcRepository<ProfileEntity, Long> {
    Flux<ProfileEntity> findAllByName(String name);

    Mono<ProfileEntity> findByNameAndKey(String name, String key);

    Mono<Integer> deleteAllByName(String name);

    Mono<Integer> deleteByNameAndKey(String name, String key);
}
