package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.store.entity.SubjectEntity;

/**
 * SubjectEntity repository.
 *
 * @author: li-guohao
 * @see SubjectEntity
 */
public interface SubjectRepository extends R2dbcRepository<SubjectEntity, Long> {
}
