package run.ikaros.authentication;

import java.util.UUID;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.authentication.api.UserExistenceQuery;

/** Authentication 对外提供的用户存在性查询适配器。 */
@Component
public class DefaultUserExistenceQuery implements UserExistenceQuery {
    private final PlatformUserRepository users;

    public DefaultUserExistenceQuery(PlatformUserRepository users) {
        this.users = users;
    }

    @Override
    public Mono<Boolean> exists(UUID userId) {
        return users.existsById(userId);
    }
}
