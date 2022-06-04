package cn.liguohao.ikaros.entity;

import cn.liguohao.ikaros.define.enums.Role;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * @author li-guohao
 * @date 2022/06/03
 */
@Entity
@Table(name = "relation")
public class RelationEntity extends BaseEntity {

    /**
     * 关系类型，对于主体来说，客体所属的角色
     */
    private Role role;

    /**
     * 主体
     */
    private Long masterUid;

    /**
     * 客体
     */
    private Long guestUid;

    public Role getRole() {
        return role;
    }

    public RelationEntity setRole(Role role) {
        this.role = role;
        return this;
    }

    public Long getMasterUid() {
        return masterUid;
    }

    public RelationEntity setMasterUid(Long masterUid) {
        this.masterUid = masterUid;
        return this;
    }

    public Long getGuestUid() {
        return guestUid;
    }

    public RelationEntity setGuestUid(Long guestUid) {
        this.guestUid = guestUid;
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RelationEntity{");
        sb.append("role=").append(role);
        sb.append(", masterUid=").append(masterUid);
        sb.append(", guestUid=").append(guestUid);
        sb.append('}');
        return sb.toString();
    }
}
