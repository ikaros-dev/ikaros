package run.ikaros.resource;

import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * 用户自定义 Resource 标签的业务能力。
 */
public interface ResourceTagService {
    /** 添加标签，重复名称保持幂等。 */
    Mono<ResourceTagView> add(UUID ownerId, UUID resourceId, CreateResourceTagRequest request);

    /** 查询资源的全部用户标签。 */
    Mono<List<ResourceTagView>> list(UUID ownerId, UUID resourceId);

    /** 删除资源上的指定用户标签。 */
    Mono<Void> remove(UUID ownerId, UUID resourceId, UUID tagId);
}
