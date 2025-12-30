package run.ikaros.server.store.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.store.entity.EpisodeListEntity;

public interface EpisodeListRepository extends R2dbcRepository<EpisodeListEntity, UUID> {
}
