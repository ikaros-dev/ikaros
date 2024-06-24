package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.UserEntity;

/**
 * UserEntity repository.
 *
 * @author: li-guohao
 * @see UserEntity
 */
public interface UserRepository extends R2dbcRepository<UserEntity, Long> {

    Mono<UserEntity> findByUsernameAndEnableAndDeleteStatus(String username,
                                                            Boolean enable,
                                                            Boolean deleteStatus);

    Mono<Boolean> existsByUsername(String username);

    Mono<Boolean> existsByEmail(String email);

    Mono<Boolean> existsByUsernameAndEnableAndDeleteStatus(String username,
                                                           Boolean enable,
                                                           Boolean deleteStatus);

    @Query("update ikuser set role_id=$2 where username=$1")
    Mono<UserEntity> updateRoleByUsername(String username, Long roleId);

    @Query("update ikuser set telephone=$2 where username=$1")
    Mono<UserEntity> updateTelephoneByUsername(String username, String telephone);

    @Query("update ikuser set email=$2 where username=$1")
    Mono<UserEntity> updateEmailByUsername(String username, String email);

}
