package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.store.entity.EpisodeFileEntity;

public interface EpisodeFileRepository extends R2dbcRepository<EpisodeFileEntity, Long> {
}
