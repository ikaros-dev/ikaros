package run.ikaros.server.subject.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.subject.store.entity.StatusEntity;

/**
 * StatusEntity repository.
 *
 * @author: li-guohao
 * @see StatusEntity
 */
public interface StatusRepository extends R2dbcRepository<StatusEntity, Long> {
}
