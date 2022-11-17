package run.ikaros.server.core.repository;


import run.ikaros.server.entity.UserEntity;

/**
 * @author liguohao
 */
public interface UserRepository extends BaseRepository<UserEntity, Long> {
    /**
     * 根据用户名查询用户
     *
     * @param username 唯一的用户名
     * @return 用户实体对象
     */
    UserEntity findByUsername(String username);

    UserEntity findByEmail(String email);
}
