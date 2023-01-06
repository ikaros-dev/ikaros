package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.store.entity.MetadataEntity;

/**
 * MetadataEntity repository.
 *
 * @author: li-guohao
 * @see MetadataEntity
 */
public interface MetadataRepository extends R2dbcRepository<MetadataEntity, Long> {
}
