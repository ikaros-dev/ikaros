package run.ikaros.server.core.binding.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.binding.DirectoryBindingContext;
import run.ikaros.api.core.binding.DirectoryBindingStep;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.core.subject.service.SubjectService;
import run.ikaros.server.core.subject.service.SubjectSyncService;

/**
 * Step 3: Fetch and create subject via SubjectSyncService using platform and platform id.
 * Order: 30
 */
@Slf4j
@Component
public class FetchAndCreateSubjectStep implements DirectoryBindingStep {

    private final SubjectSyncService subjectSyncService;
    private final SubjectService subjectService;

    public FetchAndCreateSubjectStep(SubjectSyncService subjectSyncService,
                                     SubjectService subjectService) {
        this.subjectSyncService = subjectSyncService;
        this.subjectService = subjectService;
    }

    @Override
    public String name() {
        return "FetchAndCreateSubject";
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public boolean shouldSkip(DirectoryBindingContext context) {
        return context.getPlatformId() == null || context.getPlatform() == null
            || context.getSubjectId() != null;
    }

    @Override
    public Mono<DirectoryBindingContext> execute(DirectoryBindingContext context) {
        SubjectSyncPlatform platform = context.getPlatform();
        String platformId = context.getPlatformId();

        return subjectSyncService.sync(null, platform, platformId)
            .then(subjectSyncService.findSubjectSyncsByPlatformAndPlatformId(platform, platformId)
                .next())
            .flatMap(syncRecord -> {
                context.setSubjectId(syncRecord.getSubjectId());
                context.setSubjectSync(syncRecord);
                log.info("Synced subject: id={}, platform={}, platformId={}",
                    syncRecord.getSubjectId(), platform, platformId);
                return Mono.just(context);
            });
    }

    @Override
    public Mono<Void> rollback(DirectoryBindingContext context) {
        Mono<Void> removeSync = Mono.empty();
        if (context.getSubjectSync() != null) {
            removeSync = subjectSyncService.remove(context.getSubjectSync())
                .onErrorResume(e -> {
                    log.warn("Failed to remove subject sync during rollback", e);
                    return Mono.empty();
                });
        }

        Mono<Void> removeSubject = Mono.empty();
        if (context.getSubjectId() != null) {
            removeSubject = subjectService.deleteById(context.getSubjectId())
                .onErrorResume(e -> {
                    log.warn("Failed to remove subject during rollback", e);
                    return Mono.empty();
                });
        }

        return removeSync.then(removeSubject);
    }
}
