package cn.liguohao.ikaros.model.entity;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 用户(1) -- 角色(n) 对应关系表
 *
 * @author li-guohao
 */
@Entity
@Table(name = "user_role")
public class UserRoleEntity extends BaseEntity {

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

    public UserRoleEntity setUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    public Long getRoleId() {
        return roleId;
    }

    public UserRoleEntity setRoleId(Long roleId) {
        this.roleId = roleId;
        return this;
    }
}
