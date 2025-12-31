package run.ikaros.api.core.subject;

import jakarta.annotation.Nullable;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.plugin.AllowPluginOperate;
import run.ikaros.api.store.enums.SubjectSyncPlatform;

public interface SubjectSyncOperate extends AllowPluginOperate {
    /**
     * 从第三方平台同步条目，需要插件提供底层的数据获取支持.
     *
     * @param subjectId  可以为空，为空则是拉取创建新的，不为空则是先查询数据库，存在则更新，不存在则新增.
     * @param platform   对应的平台
     * @param platformId 对应的平台ID
     */
    @Transactional
    Mono<Void> sync(@Nullable UUID subjectId, SubjectSyncPlatform platform, String platformId);

    Mono<SubjectSync> save(SubjectSync subjectSync);

    Flux<SubjectSync> findSubjectSyncsBySubjectId(UUID subjectId);

    Mono<SubjectSync> findSubjectSyncBySubjectIdAndPlatform(UUID subjectId,
                                                            SubjectSyncPlatform platform);

    Flux<SubjectSync> findSubjectSyncsByPlatformAndPlatformId(SubjectSyncPlatform platform,
                                                              String platformId);

    Mono<SubjectSync> findBySubjectIdAndPlatformAndPlatformId(UUID subjectId,
                                                              SubjectSyncPlatform platform,
                                                              String platformId);
}
