package cn.liguohao.ikaros.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * 用户(1) -- 角色(n) 对应关系表
 *
 * @author li-guohao
 */
@Entity
public class UserRole extends Base {

    /**
     * 用户唯一ID
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 角色表主键ID
     */
    @Column(name = "role_id")
    private Long roleId;

    public Long getUserId() {
        return userId;
    }

    public UserRole setUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    public Long getRoleId() {
        return roleId;
    }

    public UserRole setRoleId(Long roleId) {
        this.roleId = roleId;
        return this;
    }
}
