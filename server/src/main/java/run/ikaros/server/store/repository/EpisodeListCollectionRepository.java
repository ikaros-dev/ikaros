package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.store.entity.EpisodeListCollectionEntity;

public interface EpisodeListCollectionRepository
    extends R2dbcRepository<EpisodeListCollectionEntity, Long> {
}
