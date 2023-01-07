package run.ikaros.server.subject.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.subject.store.entity.MetadataEntity;

/**
 * MetadataEntity repository.
 *
 * @author: li-guohao
 * @see MetadataEntity
 */
public interface MetadataRepository extends R2dbcRepository<MetadataEntity, Long> {
}
