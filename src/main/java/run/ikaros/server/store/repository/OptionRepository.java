package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.store.entity.OptionEntity;

/**
 * OptionEntity repository.
 *
 * @author: li-guohao
 * @see OptionEntity
 */
public interface OptionRepository extends R2dbcRepository<OptionEntity, Long> {
}
