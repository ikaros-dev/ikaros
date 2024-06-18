package run.ikaros.server.core.subject.service.impl;

import static run.ikaros.server.infra.utils.ReactiveBeanUtils.copyProperties;

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
import reactor.core.scheduler.Schedulers;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.core.subject.SubjectSync;
import run.ikaros.api.core.subject.SubjectSyncAction;
import run.ikaros.api.core.subject.SubjectSynchronizer;
import run.ikaros.api.core.subject.vo.PostSubjectSyncCondition;
import run.ikaros.api.infra.exception.subject.NoAvailableSubjectPlatformSynchronizerException;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
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
    public synchronized Mono<Subject> sync(@Nullable Long subjectId, SubjectSyncPlatform platform,
                                           String platformId) {
        Assert.notNull(platform, "'platform' must not null.");
        Assert.hasText(platformId, "'platformId' must has text.");
        // 查询是否已经同步过了，如果已经同步过则返回对应的条目信息
        return subjectSyncRepository.findByPlatformAndPlatformId(platform, platformId)
            .collectList()
            .filter(subjectSyncEntities -> !subjectSyncEntities.isEmpty())
            .map(subjectSyncEntities -> subjectSyncEntities.get(0))
            .map(SubjectSyncEntity::getSubjectId)
            .flatMap(subjectService::findById)
            .switchIfEmpty(syncBySubjectSynchronizer(subjectId, platform, platformId));
    }

    @Override
    public Mono<Subject> sync(PostSubjectSyncCondition condition) {
        Assert.notNull(condition, "'condition' must not null.");
        Long subjectId = condition.getSubjectId();
        SubjectSyncPlatform platform = condition.getPlatform();
        String platformId = condition.getPlatformId();
        SubjectSyncAction action = condition.getSubjectSyncAction();

        if (SubjectSyncAction.PULL.equals(action)) {
            return sync(subjectId, platform, platformId);
        }

        Assert.isTrue(subjectId > 0,
            "'subjectId' must not null and gt 0 when action is MERGE.");

        return subjectService.findById(subjectId)
            .flatMapMany(subject ->
                Flux.fromStream(extensionComponentsFinder.getExtensions(SubjectSynchronizer.class)
                    .stream())
            )
            .filter(subjectSynchronizer -> platform.equals(subjectSynchronizer.getSyncPlatform()))
            .collectList()
            .filter(subjectSynchronizes -> !subjectSynchronizes.isEmpty())
            .switchIfEmpty(Mono.error(new NoAvailableSubjectPlatformSynchronizerException(
                "No found available subject platform synchronizer for platform-id: "
                    + platform.name() + "-" + platformId)))
            .map(subjectSynchronizes -> subjectSynchronizes.get(0))
            .flatMap(subjectSynchronizer -> subjectService.findById(subjectId)
                .flatMap(subject -> subjectSynchronizer.merge(subject, platformId)))
            .onErrorResume(Exception.class, e -> {
                String msg =
                    "Operate has exception "
                        + "for platform-id: "
                        + platform.name() + "-" + platformId
                        + ", plugin exception msg: " + e.getMessage();
                log.error(msg, e);
                return Mono.error(new NoAvailableSubjectPlatformSynchronizerException(msg));
            })
            .flatMap(subject -> subjectService.update(subject)
                .then(Mono.defer(() -> subjectService.findById(subjectId))))
            .subscribeOn(Schedulers.boundedElastic());

    }

    @Override
    public Mono<Subject> syncSelfAndRelations(SubjectSyncPlatform platform, String platformId) {
        Assert.notNull(platform, "'platform' must not null.");
        Assert.hasText(platformId, "'platformId' must has text.");
        // 查询是否已经同步过了，如果已经同步过则返回对应的条目信息
        return subjectSyncRepository.findByPlatformAndPlatformId(platform, platformId)
            .collectList()
            .filter(subjectSyncEntities -> !subjectSyncEntities.isEmpty())
            .map(subjectSyncEntities -> subjectSyncEntities.get(0))
            .map(SubjectSyncEntity::getSubjectId)
            .flatMap(subjectService::findById)
            .switchIfEmpty(doSyncSelfAndRelations(platform, platformId));
    }

    private Mono<Subject> doSyncSelfAndRelations(SubjectSyncPlatform platform, String platformId) {
        Assert.notNull(platform, "'platform' must not null.");
        Assert.hasText(platformId, "'platformId' must has text.");
        return Flux.fromStream(extensionComponentsFinder.getExtensions(SubjectSynchronizer.class)
                .stream())
            .filter(subjectSynchronizer -> platform.equals(subjectSynchronizer.getSyncPlatform()))
            .collectList()
            .filter(subjectSynchronizes -> !subjectSynchronizes.isEmpty())
            .switchIfEmpty(Mono.error(new NoAvailableSubjectPlatformSynchronizerException(
                "No found available subject platform synchronizer for platform-id: "
                    + platform.name() + "-" + platformId)))
            .map(subjectSynchronizes -> subjectSynchronizes.get(0))
            .flatMap(subjectSynchronizer -> subjectSynchronizer.pullSelfAndRelations(platformId))
            .onErrorResume(Exception.class, e -> {
                String msg =
                    "Operate has exception "
                        + "for platform-id: "
                        + platform.name() + "-" + platformId
                        + ", plugin exception msg: " + e.getMessage();
                log.error(msg, e);
                return Mono.error(new NoAvailableSubjectPlatformSynchronizerException(msg));
            }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<SubjectSync> save(SubjectSync subjectSync) {
        log.debug("save: {}", subjectSync);
        return subjectSyncRepository.findBySubjectIdAndPlatformAndPlatformId(
                subjectSync.getSubjectId(), subjectSync.getPlatform(), subjectSync.getPlatformId())
            .switchIfEmpty(Mono.just(new SubjectSyncEntity()
                    .setSubjectId(subjectSync.getSubjectId())
                    .setPlatform(subjectSync.getPlatform())
                    .setPlatformId(subjectSync.getPlatformId())
                    .setSyncTime(subjectSync.getSyncTime()))
                .doOnSuccess(e -> log.debug("create new subject sync record: [{}].", e)))
            .map(entity -> entity.setSubjectId(subjectSync.getSubjectId())
                .setPlatform(subjectSync.getPlatform())
                .setPlatformId(subjectSync.getPlatformId())
                .setSyncTime(subjectSync.getSyncTime()))
            .flatMap(subjectSyncRepository::save)
            .map(entity -> subjectSync
                .setSubjectId(entity.getSubjectId())
                .setPlatform(entity.getPlatform())
                .setPlatformId(entity.getPlatformId())
                .setSyncTime(entity.getSyncTime()));
    }

    @Override
    public Mono<Void> remove(SubjectSync subjectSync) {
        return copyProperties(subjectSync, SubjectSyncEntity.builder().build())
            .flatMap(subjectSyncRepository::delete);
    }

    @Override
    public Flux<SubjectSync> findSubjectSyncsBySubjectId(long subjectId) {
        Assert.isTrue(subjectId > 0, "'subjectId' must gt 0.");
        return subjectSyncRepository.findAllBySubjectId(subjectId)
            .flatMap(subjectSyncEntity -> copyProperties(subjectSyncEntity,
                SubjectSync.builder().build()));
    }

    @Override
    public Mono<SubjectSync> findSubjectSyncBySubjectIdAndPlatform(long subjectId,
                                                                   SubjectSyncPlatform platform) {
        Assert.isTrue(subjectId > 0, "'subjectId' must gt 0.");
        Assert.notNull(platform, "'platform' must not null.");
        return subjectSyncRepository.findBySubjectIdAndPlatform(subjectId, platform)
            .flatMap(subjectSyncEntity -> copyProperties(subjectSyncEntity,
                SubjectSync.builder().build()));
    }

    @Override
    public Flux<SubjectSync> findSubjectSyncsByPlatformAndPlatformId(SubjectSyncPlatform platform,
                                                                     String platformId) {
        Assert.notNull(platform, "'platform' must not null.");
        Assert.hasText(platformId, "'platformId' must has text.");
        return subjectSyncRepository.findByPlatformAndPlatformId(platform, platformId)
            .flatMap(subjectSyncEntity -> copyProperties(subjectSyncEntity,
                SubjectSync.builder().build()));
    }

    @Override
    public Mono<SubjectSync> findBySubjectIdAndPlatformAndPlatformId(Long subjectId,
                                                                     SubjectSyncPlatform platform,
                                                                     String platformId) {
        Assert.isTrue(subjectId > 0, "'subjectId' must gt 0.");
        Assert.notNull(platform, "'platform' must not null.");
        Assert.hasText(platformId, "'platformId' must has text.");
        return subjectSyncRepository.findBySubjectIdAndPlatformAndPlatformId(
                subjectId, platform, platformId)
            .flatMap(subjectSyncEntity -> copyProperties(subjectSyncEntity,
                SubjectSync.builder().build()));
    }


    private Mono<Subject> syncBySubjectSynchronizer(@Nullable Long subjectId,
                                                    SubjectSyncPlatform platform,
                                                    String platformId) {
        return Flux.fromStream(extensionComponentsFinder.getExtensions(SubjectSynchronizer.class)
                .stream())
            .filter(subjectSynchronizer -> platform.equals(subjectSynchronizer.getSyncPlatform()))
            .collectList()
            .filter(subjectSynchronizes -> !subjectSynchronizes.isEmpty())
            .switchIfEmpty(Mono.error(new NoAvailableSubjectPlatformSynchronizerException(
                "No found available subject platform synchronizer for platform-id: "
                    + platform.name() + "-" + platformId)))
            .map(subjectSynchronizes -> subjectSynchronizes.get(0))
            .flatMap(subjectSynchronizer -> subjectSynchronizer.pull(platformId))
            .onErrorResume(Exception.class, e -> {
                String msg =
                    "Operate has exception "
                        + "for platform-id: "
                        + platform.name() + "-" + platformId
                        + ", plugin exception msg: " + e.getMessage();
                log.error(msg, e);
                return Mono.error(new NoAvailableSubjectPlatformSynchronizerException(msg));
            })
            .flatMap(subject -> Objects.isNull(subjectId)
                ? subjectService.create(subject)
                .onErrorResume(DuplicateKeyException.class, e -> Mono.just(subject)
                    .map(sub -> sub.setId(null)).flatMap(subjectService::create))
                : subjectService.update(subject)
                .then(Mono.defer(() -> subjectService.findById(subjectId))))
            .subscribeOn(Schedulers.boundedElastic());
    }


    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext)
        throws BeansException {
        this.applicationContext = applicationContext;
    }
}
