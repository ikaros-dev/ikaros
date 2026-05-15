package run.ikaros.server.store.repository;

import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.CharacterEntity;

public interface CharacterRepository extends BaseRepository<CharacterEntity> {
    Mono<CharacterEntity> findByName(String name);
}
