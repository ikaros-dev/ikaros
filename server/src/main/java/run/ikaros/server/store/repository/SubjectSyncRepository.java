package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.store.entity.SubjectSyncEntity;

public interface SubjectSyncRepository extends R2dbcRepository<SubjectSyncEntity, Long> {
    Mono<SubjectSyncEntity> findByPlatformAndPlatformId(SubjectSyncPlatform platform,
                                                        String platformId);
}
