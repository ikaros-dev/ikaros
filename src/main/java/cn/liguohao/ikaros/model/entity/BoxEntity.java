package cn.liguohao.ikaros.model.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * @author liguohao
 */
@Entity
@Table(name = "role", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"parent_id", "name"})})
public class BoxEntity extends BaseEntity {
    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "type_id")
    private Long typeId;

    /**
     * 盒子名称，要求兄弟盒子不允许重名，即当 parent_id 相同时，不允许 name 重复
     */
    private String name;

    public Long getParentId() {
        return parentId;
    }

    public BoxEntity setParentId(Long parentId) {
        this.parentId = parentId;
        return this;
    }

    public Long getTypeId() {
        return typeId;
    }

    public BoxEntity setTypeId(Long typeId) {
        this.typeId = typeId;
        return this;
    }

    public String getName() {
        return name;
    }

    public BoxEntity setName(String name) {
        this.name = name;
        return this;
    }
}