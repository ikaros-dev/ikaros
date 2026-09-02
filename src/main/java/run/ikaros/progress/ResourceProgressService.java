package run.ikaros.progress;

import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * 统一消费进度的记录与查询能力。
 */
public interface ResourceProgressService {
    /** 设置或更新当前用户在 Resource 上指定类型的进度。 */
    Mono<ResourceProgressView> set(UUID ownerId, UUID resourceId, SetProgressRequest request);

    /** 查询当前用户在 Resource 上指定类型的进度。 */
    Mono<ResourceProgressView> get(UUID ownerId, UUID resourceId, ProgressType type);
}
