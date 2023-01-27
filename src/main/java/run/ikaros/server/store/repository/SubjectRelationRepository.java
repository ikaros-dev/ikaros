package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.store.entity.SubjectRelationEntity;

public interface SubjectRelationRepository extends R2dbcRepository<SubjectRelationEntity, Long> {
}
