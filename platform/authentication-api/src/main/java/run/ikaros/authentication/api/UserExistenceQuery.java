package run.ikaros.authentication.api;

import java.util.UUID;
import reactor.core.publisher.Mono;

/** 查询用户是否存在的最小公开能力，不暴露 Authentication 持久化模型。 */
public interface UserExistenceQuery {
    Mono<Boolean> exists(UUID userId);
}
