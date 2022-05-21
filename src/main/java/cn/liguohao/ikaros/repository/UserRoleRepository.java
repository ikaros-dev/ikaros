package cn.liguohao.ikaros.repository;



import static cn.liguohao.ikaros.config.CacheConfig.APP_CACHE_NAME;

import cn.liguohao.ikaros.entity.UserRole;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author liguohao
 */
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    /**
     * 根据用户ID查询用户角色关系记录
     *
     * @param userId 用户ID
     * @return 用户角色关系记录集合
     */
    @Cacheable(APP_CACHE_NAME)
    List<UserRole> findByUserId(Long userId);
}
