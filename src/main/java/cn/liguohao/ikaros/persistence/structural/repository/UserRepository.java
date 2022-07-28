package cn.liguohao.ikaros.persistence.structural.repository;


import cn.liguohao.ikaros.persistence.structural.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author liguohao
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    /**
     * 根据用户名查询用户
     *
     * @param username 唯一的用户名
     * @return 用户实体对象
     */
    UserEntity findByUsername(String username);
}
