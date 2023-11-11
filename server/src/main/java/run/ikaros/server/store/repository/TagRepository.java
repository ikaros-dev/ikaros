package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.store.entity.TagEntity;

public interface TagRepository
    extends R2dbcRepository<TagEntity, Long> {

}
