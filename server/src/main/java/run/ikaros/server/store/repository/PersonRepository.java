package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.PersonEntity;

public interface PersonRepository extends BaseRepository<PersonEntity> {
    Mono<PersonEntity> findByName(String name);

    @Query("select count(*) from person where delete_status = false or delete_status is null")
    Mono<Long> countActive();
}
