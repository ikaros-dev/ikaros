package cn.liguohao.ikaros.model.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author guohao
 * @date 2022/09/10
 */
@Entity
@Table(name = "resource_type")
public class ResourceTypeEntity extends BaseEntity {

    @Column(name = "parent_id")
    private Long parentId;

    /**
     * 资源类型的名称，不允许重名
     */
    @Column(unique = true)
    private String name;

    public Long getParentId() {
        return parentId;
    }

    public ResourceTypeEntity setParentId(Long parentId) {
        this.parentId = parentId;
        return this;
    }

    public String getName() {
        return name;
    }

    public ResourceTypeEntity setName(String name) {
        this.name = name;
        return this;
    }
}
