package run.ikaros.activity;

import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * 用户 Resource Activity 的记录、查询和删除能力。
 */
public interface ResourceActivityService {
    /** 记录当前用户对 Resource 的一次活动。 */
    Mono<ResourceActivityView> record(UUID ownerId, UUID resourceId, RecordActivityRequest request);

    /** 查询当前用户最近的活动，limit 由服务层约束。 */
    Mono<List<ResourceActivityView>> recent(UUID ownerId, int limit);

    /** 删除当前用户的一条可删除 Activity。 */
    Mono<Void> delete(UUID ownerId, UUID activityId);
}
