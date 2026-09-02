package run.ikaros.resource;

import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Resource 多语言标题的维护能力。
 */
public interface ResourceTitleService {

    /**
     * 新增或修改 Resource 指定语言的标题。
     *
     * @param ownerId 当前拥有者标识
     * @param resourceId Resource 标识
     * @param request 标题语言、内容和主标题标志
     * @return 保存后的标题
     */
    Mono<ResourceTitleView> set(UUID ownerId, UUID resourceId, SetResourceTitleRequest request);

    /**
     * 删除一个标题；删除主标题时会自动提升另一个标题。
     *
     * @param ownerId 当前拥有者标识
     * @param resourceId Resource 标识
     * @param titleId 标题标识
     * @return 删除完成信号
     */
    Mono<Void> delete(UUID ownerId, UUID resourceId, UUID titleId);
}
