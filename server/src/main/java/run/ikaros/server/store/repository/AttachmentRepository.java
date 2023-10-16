package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.store.entity.AttachmentEntity;

public interface AttachmentRepository extends R2dbcRepository<AttachmentEntity, Long> {
}
