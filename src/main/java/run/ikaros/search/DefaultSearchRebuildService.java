package run.ikaros.search;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** 以业务查询结果为输入执行有界、可继续的全量投影重建。 */
@Service
public class DefaultSearchRebuildService implements SearchRebuildService {
    private final SearchProjectionService projectionService;

    public DefaultSearchRebuildService(SearchProjectionService projectionService) {
        this.projectionService = projectionService;
    }

    @Override
    public Mono<SearchRebuildResult> rebuild(Flux<SearchProjectionInput> source, String projectorVersion) {
        if (projectorVersion == null || projectorVersion.isBlank()) {
            return Mono.error(new IllegalArgumentException("projector version 不能为空"));
        }
        return projectionService.startRebuild().flatMap(generation -> source
            .concatMap(input -> project(input, projectorVersion, generation))
            .reduce(new Counts(generation, 0, 0), Counts::add))
            .map(counts -> new SearchRebuildResult(counts.generation, counts.projected, counts.failed));
    }

    private Mono<Counts> project(SearchProjectionInput input, String projectorVersion, long generation) {
        return projectionService.project(input.sourceId(), input.sourceVersion(), input.fields(),
                projectorVersion, generation)
            .thenReturn(new Counts(generation, 1, 0))
            .onErrorResume(error -> projectionService.recordFailure(input.sourceId(), input.sourceVersion(),
                    generation, error.getMessage()).thenReturn(new Counts(generation, 0, 1)));
    }

    private static final class Counts {
        private long generation;
        private long projected;
        private long failed;

        private Counts() { }

        private Counts(long generation, long projected, long failed) {
            this.generation = generation;
            this.projected = projected;
            this.failed = failed;
        }

        private Counts add(Counts other) {
            generation = other.generation;
            projected += other.projected;
            failed += other.failed;
            return this;
        }
    }
}
