package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
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
    extends R2dbcRepository<CustomMetadataEntity, Long> {
    Mono<Void> deleteAllByCustomId(Long customId);

    Flux<CustomMetadataEntity> findAllByCustomId(Long customId);

    Mono<CustomMetadataEntity> findByCustomIdAndKey(Long customId, String key);

    @Query("update custom_metadata set cm_value=:value "
        + "where custom_id = :customId and cm_key=:key")
    Mono<Void> updateValueByCustomIdAndKeyAndValue(@Param("customId") Long customId,
                                                   @Param("key") String key,
                                                   @Param("value") byte[] value);

}
