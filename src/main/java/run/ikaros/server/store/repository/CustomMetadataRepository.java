package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.CustomMetadataEntity;

/**
 * CustomMetadataEntity repository.
 *
 * @author: li-guohao
 * @see CustomMetadataEntity
 */
public interface CustomMetadataRepository
    extends R2dbcRepository<CustomMetadataEntity, Long> {
    Mono<Void> deleteAllByCustomId(Long customId);

    Mono<CustomMetadataEntity> findByCustomIdAndKey(Long customId, String key);
}
