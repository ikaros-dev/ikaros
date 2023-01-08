package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.store.entity.CustomEntity;

/**
 * CustomEntity repository.
 *
 * @author: li-guohao
 * @see CustomEntity
 */
public interface CustomRepository extends R2dbcRepository<CustomEntity, Long> {
}
