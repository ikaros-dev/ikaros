package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.store.entity.BoxEntity;

/**
 * BoxEntity repository.
 *
 * @author: li-guohao
 * @see BoxEntity
 */
public interface BoxRepository extends R2dbcRepository<BoxEntity, Long> {
}
