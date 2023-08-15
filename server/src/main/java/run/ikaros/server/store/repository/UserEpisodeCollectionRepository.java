package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.store.entity.UserEpisodeCollectionEntity;

public interface UserEpisodeCollectionRepository
    extends R2dbcRepository<UserEpisodeCollectionEntity, Long> {
}
