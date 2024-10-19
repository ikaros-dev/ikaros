package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.PersonEntity;

public interface PersonRepository extends R2dbcRepository<PersonEntity, Long> {
    Mono<PersonEntity> findByName(String name);
}
