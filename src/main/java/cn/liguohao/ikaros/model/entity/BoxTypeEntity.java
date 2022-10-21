package cn.liguohao.ikaros.model.entity;

import cn.liguohao.ikaros.common.constants.InitConstants;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "box_type", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"parent_id", "name"})})
public class BoxTypeEntity extends BaseEntity {

    @Column(name = "parent_id", nullable = false)
    private Long parentId = InitConstants.ROOT_ID;;

    /**
     * 盒子类型的名称，不允许重名
     */
    @Column(nullable = false)
    private String name;

    public Long getParentId() {
        return parentId;
    }

    public BoxTypeEntity setParentId(Long parentId) {
        this.parentId = parentId;
        return this;
    }

    public String getName() {
        return name;
    }

    public BoxTypeEntity setName(String name) {
        this.name = name;
        return this;
    }
}
