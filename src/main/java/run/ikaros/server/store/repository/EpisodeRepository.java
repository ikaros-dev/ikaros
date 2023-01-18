package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.store.entity.EpisodeEntity;

public interface EpisodeRepository extends R2dbcRepository<EpisodeEntity, Long> {
}
