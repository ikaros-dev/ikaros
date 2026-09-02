package run.ikaros.resource;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * 多语言 Resource 标题的数据库访问边界。
 */
public interface ResourceTitleRepository extends ReactiveCrudRepository<ResourceTitleEntity, UUID> {

    /**
     * 查询资源的所有标题。
     *
     * @param resourceId Resource 标识
     * @return 标题列表
     */
    Flux<ResourceTitleEntity> findAllByResourceIdOrderByPrimaryDescLocaleAsc(UUID resourceId);
}
