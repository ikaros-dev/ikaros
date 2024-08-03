package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.store.entity.EpisodeListEpisodeEntity;

public interface EpisodeListEpisodeRepository
    extends R2dbcRepository<EpisodeListEpisodeEntity, Long> {
}
