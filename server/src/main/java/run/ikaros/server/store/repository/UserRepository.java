package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.store.entity.UserEntity;

/**
 * UserEntity repository.
 *
 * @author: li-guohao
 * @see UserEntity
 */
public interface UserRepository extends R2dbcRepository<UserEntity, Long> {
}
