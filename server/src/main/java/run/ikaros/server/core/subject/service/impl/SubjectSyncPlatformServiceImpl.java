package run.ikaros.server.core.subject.service.impl;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.core.subject.SubjectSynchronizer;
import run.ikaros.api.exception.NoAvailableSubjectPlatformSynchronizerException;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.core.subject.event.SubjectCoverImageDownloadAndUpdateEvent;
import run.ikaros.server.core.subject.service.SubjectService;
import run.ikaros.server.core.subject.service.SubjectSyncPlatformService;
import run.ikaros.server.plugin.ExtensionComponentsFinder;
import run.ikaros.server.store.entity.SubjectSyncEntity;
import run.ikaros.server.store.repository.SubjectSyncRepository;

@Slf4j
@Service
public class SubjectSyncPlatformServiceImpl implements SubjectSyncPlatformService,
    ApplicationContextAware {
    private final ExtensionComponentsFinder extensionComponentsFinder;
    private final SubjectService subjectService;
    private ApplicationContext applicationContext;
    private final SubjectSyncRepository subjectSyncRepository;

    /**
     * Construct.
     */
    public SubjectSyncPlatformServiceImpl(ExtensionComponentsFinder extensionComponentsFinder,
                                          SubjectService subjectService,
                                          SubjectSyncRepository subjectSyncRepository) {
        this.extensionComponentsFinder = extensionComponentsFinder;
        this.subjectService = subjectService;
        this.subjectSyncRepository = subjectSyncRepository;
    }

    @Override
    public Mono<Subject> sync(@Nullable Long subjectId, SubjectSyncPlatform platform,
                              String platformId) {
        Assert.notNull(platform, "'platform' must not null.");
        Assert.hasText(platformId, "'platformId' must has text.");
        // 查询是否已经同步过了，如果已经同步过则返回对应的条目信息
        return subjectSyncRepository.findByPlatformAndPlatformId(platform, platformId)
            .map(SubjectSyncEntity::getSubjectId)
            .flatMap(subjectService::findById)
            .switchIfEmpty(syncBySubjectSynchronizer(subjectId, platform, platformId));
    }

    private Mono<Subject> syncBySubjectSynchronizer(@Nullable Long subjectId,
                                                    SubjectSyncPlatform platform,
                                                    String platformId) {
        return Flux.fromStream(extensionComponentsFinder.getExtensions(SubjectSynchronizer.class)
                .stream())
            .filter(subjectSynchronizer -> platform.equals(subjectSynchronizer.getSyncPlatform()))
            .collectList()
            .filter(subjectSynchronizes -> subjectSynchronizes.size() > 0)
            .switchIfEmpty(Mono.error(new NoAvailableSubjectPlatformSynchronizerException(
                "No found available subject platform synchronizer for platform-id: "
                    + platform.name() + "-" + platformId)))
            .map(subjectSynchronizes -> subjectSynchronizes.get(0))
            .map(subjectSynchronizer -> subjectSynchronizer.pull(platformId))
            .onErrorResume(Exception.class, e ->
                Mono.error(new NoAvailableSubjectPlatformSynchronizerException(
                    "Operate not available, platform api domain can not access for platform-id: "
                        + platform.name() + "-" + platformId
                        + ", plugin exception msg: " + e.getMessage())))
            .flatMap(subject -> Objects.isNull(subjectId)
                ? subjectService.create(subject)
                .onErrorResume(DuplicateKeyException.class, e -> Mono.just(subject)
                    .map(sub -> sub.setId(null)).flatMap(subjectService::create))
                : subjectService.update(subject)
                .then(Mono.defer(() -> subjectService.findById(subjectId))))
            .doOnSuccess(subject -> applicationContext.publishEvent(
                new SubjectCoverImageDownloadAndUpdateEvent(this,
                    subject.getId(), subject.getCover())));
    }


    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext)
        throws BeansException {
        this.applicationContext = applicationContext;
    }
}
