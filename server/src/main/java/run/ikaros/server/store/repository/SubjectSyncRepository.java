package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.store.entity.SubjectSyncEntity;

public interface SubjectSyncRepository extends R2dbcRepository<SubjectSyncEntity, Long> {
    Flux<SubjectSyncEntity> findByPlatformAndPlatformId(SubjectSyncPlatform platform,
                                                        String platformId);

    Mono<SubjectSyncEntity> findBySubjectIdAndPlatform(Long subjectId,
                                                       SubjectSyncPlatform platform);

    Mono<SubjectSyncEntity> findBySubjectIdAndPlatformAndPlatformId(Long subjectId,
                                                                    SubjectSyncPlatform platform,
                                                                    String platformId);

    Mono<Boolean> existsBySubjectIdAndPlatformAndPlatformId(Long subjectId,
                                                            SubjectSyncPlatform platform,
                                                            String platformId);

    Mono<Boolean> existsByPlatformAndPlatformId(SubjectSyncPlatform platform, String platformId);

    Flux<SubjectSyncEntity> findAllBySubjectId(Long subjectId);

    Mono<Long> deleteAllBySubjectId(Long subjectId);
}
