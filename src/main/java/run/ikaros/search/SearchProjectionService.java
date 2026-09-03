package run.ikaros.search;

import java.util.Map;
import java.util.UUID;
import reactor.core.publisher.Mono;

/** Search 投影公开边界；业务模块只能提交投影输入，不能从投影读取业务状态。 */
public interface SearchProjectionService {
    Mono<SearchDocument> project(UUID sourceId, long sourceVersion, Map<String, Object> fields,
                                 String projectorVersion, long rebuildGeneration);
    Mono<Long> startRebuild();
    Mono<SearchDocument> get(UUID sourceId);
    Mono<ProjectionFailure> recordFailure(UUID sourceId, long sourceVersion,
                                           long rebuildGeneration, String reason);
}
