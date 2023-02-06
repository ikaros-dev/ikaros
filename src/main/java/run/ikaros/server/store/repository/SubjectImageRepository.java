package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.store.entity.SubjectEntity;

public interface SubjectImageRepository extends R2dbcRepository<SubjectEntity, Long> {
}
