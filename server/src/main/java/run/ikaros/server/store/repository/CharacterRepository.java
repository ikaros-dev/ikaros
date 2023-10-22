package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.store.entity.CharacterEntity;

public interface CharacterRepository extends R2dbcRepository<CharacterEntity, Long> {
}
