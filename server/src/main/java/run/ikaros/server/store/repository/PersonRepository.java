package run.ikaros.server.store.repository;

import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.PersonEntity;

public interface PersonRepository extends BaseRepository<PersonEntity> {
    Mono<PersonEntity> findByName(String name);
}
