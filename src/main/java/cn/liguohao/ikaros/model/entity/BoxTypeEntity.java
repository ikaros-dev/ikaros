package cn.liguohao.ikaros.model.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "box_type")
public class BoxTypeEntity extends BaseEntity {

    @Column(name = "parent_id")
    private Long parentId;

    /**
     * 盒子类型的名称，不允许重名
     */
    @Column(unique = true)
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
