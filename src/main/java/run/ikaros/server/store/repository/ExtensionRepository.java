package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.store.entity.ExtensionEntity;

/**
 * ExtensionEntity repository.
 *
 * @author: li-guohao
 * @see ExtensionEntity
 */
public interface ExtensionRepository extends R2dbcRepository<ExtensionEntity, Long> {
}
