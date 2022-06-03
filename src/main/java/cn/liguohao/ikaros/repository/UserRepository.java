package cn.liguohao.ikaros.repository;


import cn.liguohao.ikaros.entity.User;
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
    User findByUsername(String username);
}
