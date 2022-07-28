package cn.liguohao.ikaros.persistence.structural.repository;


import cn.liguohao.ikaros.persistence.structural.entity.UserRoleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author liguohao
 */
public interface UserRoleRepository extends JpaRepository<UserRoleEntity, Long> {

    /**
     * 根据用户ID查询用户角色关系记录
     *
     * @param userId 用户ID
     * @return 用户角色关系记录集合
     */
    List<UserRoleEntity> findByUserId(Long userId);
}
