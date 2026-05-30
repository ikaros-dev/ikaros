package run.ikaros.server.core.binding.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.binding.DirectoryBindingContext;
import run.ikaros.api.core.binding.DirectoryBindingStep;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.core.subject.service.SubjectSyncService;

/**
 * Step 1.5: If a platform ID is already set (e.g. from API keyword param),
 * sync the subject from the metadata platform before keyword search.
 * Order: 15
 */
@Slf4j
@Component
public class SyncSubjectByPlatformIdStep implements DirectoryBindingStep {

    private final SubjectSyncService subjectSyncService;

    public SyncSubjectByPlatformIdStep(SubjectSyncService subjectSyncService) {
        this.subjectSyncService = subjectSyncService;
    }

    @Override
    public String name() {
        return "SyncSubjectByPlatformId";
    }

    @Override
    public int order() {
        return 15;
    }

    @Override
    public boolean shouldSkip(DirectoryBindingContext context) {
        return context.getPlatformId() == null || context.getPlatformId().isBlank();
    }

    @Override
    public Mono<DirectoryBindingContext> execute(DirectoryBindingContext context) {
        SubjectSyncPlatform platform = context.getPlatform();
        String platformId = context.getPlatformId();

        return subjectSyncService
            .findSubjectSyncsByPlatformAndPlatformId(platform, platformId)
            .next()
            .flatMap(syncRecord -> {
                context.setSubjectId(syncRecord.getSubjectId());
                context.setSubjectSync(syncRecord);
                log.info("Found existing subject sync: id={}, platform={}, platformId={}",
                    syncRecord.getSubjectId(), platform, platformId);
                return Mono.just(context);
            })
            .switchIfEmpty(Mono.defer(() ->
                subjectSyncService.sync(null, platform, platformId)
                    .then(subjectSyncService
                        .findSubjectSyncsByPlatformAndPlatformId(platform, platformId)
                        .next())
                    .flatMap(syncRecord -> {
                        context.setSubjectId(syncRecord.getSubjectId());
                        context.setSubjectSync(syncRecord);
                        log.info("Synced subject by platform ID: id={}, platform={}, platformId={}",
                            syncRecord.getSubjectId(), platform, platformId);
                        return Mono.just(context);
                    })
            ));
    }

    @Override
    public Mono<Void> rollback(DirectoryBindingContext context) {
        if (context.getSubjectSync() == null) {
            return Mono.empty();
        }
        return subjectSyncService.remove(context.getSubjectSync())
            .onErrorResume(e -> {
                log.warn("Failed to remove subject sync during rollback", e);
                return Mono.empty();
            });
    }
}
