package cn.liguohao.ikaros.repository;


import static cn.liguohao.ikaros.config.CacheConfig.APP_CACHE_NAME;

import cn.liguohao.ikaros.entity.User;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author liguohao
 */
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * 根据用户名查询用户
     *
     * @param username 唯一的用户名
     * @return 用户实体对象
     */
    @Cacheable(value = APP_CACHE_NAME, key = "#username")
    User findByUsername(String username);

    /**
     * 覆盖保存方法，加个缓存移除注解
     *
     * @param entity must not be {@literal null}.
     * @param <S>    待保存的实体类型
     * @return 保存后的实体
     */
    @Override
    @CacheEvict(value = APP_CACHE_NAME, key = "#entity.getUsername()")
    <S extends User> S save(S entity);

    /**
     * 覆盖移除方法，加个缓存移除注解
     *
     * @param entity must not be {@literal null}.
     */
    @Override
    @CacheEvict(value = APP_CACHE_NAME, key = "#entity.getUsername()")
    void delete(User entity);
}
