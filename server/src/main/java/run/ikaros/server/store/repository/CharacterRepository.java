package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.CharacterEntity;

public interface CharacterRepository extends BaseRepository<CharacterEntity> {
    Mono<CharacterEntity> findByName(String name);

    @Query("select count(*) from character where delete_status = false or delete_status is null")
    Mono<Long> countActive();
}
