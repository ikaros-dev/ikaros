package run.ikaros.server.store.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.UserTotpEntity;

/**
 * TOTP配置仓库.
 */
public interface UserTotpRepository extends R2dbcRepository<UserTotpEntity, UUID> {
    Mono<UserTotpEntity> findByUserId(UUID userId);
}
