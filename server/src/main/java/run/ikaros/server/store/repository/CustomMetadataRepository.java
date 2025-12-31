package run.ikaros.server.store.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.CustomMetadataEntity;

/**
 * CustomMetadataEntity repository.
 *
 * @author: li-guohao
 * @see CustomMetadataEntity
 */
public interface CustomMetadataRepository
    extends R2dbcRepository<CustomMetadataEntity, UUID> {
    Mono<Void> deleteAllByCustomId(UUID customId);

    Flux<CustomMetadataEntity> findAllByCustomId(UUID customId);

    Mono<CustomMetadataEntity> findByCustomIdAndKey(UUID customId, String key);

    @Query("update custom_metadata set cm_value=$3 "
        + "where custom_id=$1 and cm_key=$2")
    Mono<Void> updateValueByCustomIdAndKeyAndValue(UUID customId, String key, byte[] value);

}
