package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.store.entity.FolderEntity;

/**
 * FolderEntity repository.
 *
 * @author: li-guohao
 * @see FolderEntity
 */
public interface FolderRepository extends R2dbcRepository<FolderEntity, Long> {
}
