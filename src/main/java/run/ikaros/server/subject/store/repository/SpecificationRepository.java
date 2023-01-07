package run.ikaros.server.subject.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.subject.store.entity.SpecificationEntity;

/**
 * SpecificationEntity repository.
 *
 * @author: li-guohao
 * @see SpecificationEntity
 */
public interface SpecificationRepository extends R2dbcRepository<SpecificationEntity, Long> {
}
