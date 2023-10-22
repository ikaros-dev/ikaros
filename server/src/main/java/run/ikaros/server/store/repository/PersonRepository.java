package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.store.entity.PersonEntity;

public interface PersonRepository extends R2dbcRepository<PersonEntity, Long> {
}
