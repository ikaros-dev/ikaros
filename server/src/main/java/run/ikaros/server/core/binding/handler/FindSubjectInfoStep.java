package run.ikaros.server.core.binding.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.binding.DirectoryBindingContext;
import run.ikaros.api.core.binding.DirectoryBindingStep;
import run.ikaros.server.core.meta.MetaInfoService;

/**
 * Step 2: Search for subject info from third-party metadata platform using clean name.
 * Order: 20
 */
@Slf4j
@Component
public class FindSubjectInfoStep implements DirectoryBindingStep {

    private final MetaInfoService metaInfoService;

    public FindSubjectInfoStep(MetaInfoService metaInfoService) {
        this.metaInfoService = metaInfoService;
    }

    @Override
    public String name() {
        return "FindSubjectInfo";
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public boolean shouldSkip(DirectoryBindingContext context) {
        return context.getPlatform() == null;
    }

    @Override
    public Mono<DirectoryBindingContext> execute(DirectoryBindingContext context) {
        String searchKeyword = context.getKeyword() != null && !context.getKeyword().isBlank()
            ? context.getKeyword() : context.getCleanName();
        return metaInfoService.searchSubjects(context.getPlatform(), searchKeyword)
            .collectList()
            .filter(list -> !list.isEmpty())
            .map(list -> list.get(0))
            .flatMap(record -> {
                if (record.subject() != null && record.syncs() != null
                    && !record.syncs().isEmpty()) {
                    String platformId = record.syncs().get(0).getPlatformId();
                    context.setPlatformId(platformId);
                    log.info("Found subject info: name={}, platformId={}",
                        record.subject().getName(), platformId);
                }
                return Mono.just(context);
            })
            .switchIfEmpty(Mono.defer(() -> {
                log.warn("No subject found for keyword: {}", context.getCleanName());
                return Mono.just(context);
            }));
    }

    @Override
    public Mono<Void> rollback(DirectoryBindingContext context) {
        return Mono.empty();
    }
}
