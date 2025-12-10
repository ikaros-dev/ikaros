package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.store.entity.AttachmentDriverEntity;

public interface AttachmentDriverRepository
    extends R2dbcRepository<AttachmentDriverEntity, Long> {
}
