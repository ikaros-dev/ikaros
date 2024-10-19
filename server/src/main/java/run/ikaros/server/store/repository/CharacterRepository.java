package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.CharacterEntity;

public interface CharacterRepository extends R2dbcRepository<CharacterEntity, Long> {
    Mono<CharacterEntity> findByName(String name);
}
