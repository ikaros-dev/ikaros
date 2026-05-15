package run.ikaros.server.store.repository;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.store.entity.SubjectSyncEntity;

public interface SubjectSyncRepository extends BaseRepository<SubjectSyncEntity> {
    Flux<SubjectSyncEntity> findByPlatformAndPlatformId(SubjectSyncPlatform platform,
                                                        String platformId);

    Mono<SubjectSyncEntity> findBySubjectIdAndPlatform(UUID subjectId,
                                                       SubjectSyncPlatform platform);

    Mono<SubjectSyncEntity> findBySubjectIdAndPlatformAndPlatformId(UUID subjectId,
                                                                    SubjectSyncPlatform platform,
                                                                    String platformId);

    Mono<Boolean> existsBySubjectIdAndPlatformAndPlatformId(UUID subjectId,
                                                            SubjectSyncPlatform platform,
                                                            String platformId);

    Mono<Boolean> existsByPlatformAndPlatformId(SubjectSyncPlatform platform, String platformId);

    Flux<SubjectSyncEntity> findAllBySubjectId(UUID subjectId);

    Mono<Long> deleteAllBySubjectId(UUID subjectId);
}
