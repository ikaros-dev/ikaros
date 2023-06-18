package run.ikaros.server.core.subject;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.core.subject.SubjectImage;
import run.ikaros.api.core.subject.SubjectSynchronizer;
import run.ikaros.api.exception.NoAvailableSubjectPlatformSynchronizerException;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.core.subject.event.SubjectImageDownloadAndUpdateEvent;
import run.ikaros.server.plugin.ExtensionComponentsFinder;

@Slf4j
@Service
public class SubjectSyncPlatformServiceImpl implements SubjectSyncPlatformService,
    ApplicationContextAware {
    private final ExtensionComponentsFinder extensionComponentsFinder;
    private final SubjectService subjectService;
    private ApplicationContext applicationContext;

    /**
     * Construct.
     */
    public SubjectSyncPlatformServiceImpl(ExtensionComponentsFinder extensionComponentsFinder,
                                          SubjectService subjectService) {
        this.extensionComponentsFinder = extensionComponentsFinder;
        this.subjectService = subjectService;
    }

    @Override
    public Mono<Subject> sync(@Nullable Long subjectId, SubjectSyncPlatform platform,
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
            .flatMap(subject -> Objects.isNull(subjectId)
                ? subjectService.create(subject)
                : subjectService.update(subject)
                .then(Mono.defer(() -> subjectService.findById(subjectId))))
            .doOnSuccess(subject -> {
                Long id = subject.getId();
                SubjectImage image = subject.getImage();
                applicationContext.publishEvent(
                    new SubjectImageDownloadAndUpdateEvent(this, id, image));
            });
    }

    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext)
        throws BeansException {
        this.applicationContext = applicationContext;
    }
}
