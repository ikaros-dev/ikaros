package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.store.entity.CollectionEntity;

public interface CollectionRepository extends R2dbcRepository<CollectionEntity, Long> {
}
