package cn.liguohao.ikaros.model.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "resource")
public class ResourceEntity extends BaseEntity {

    @Column(name = "parent_id")
    private Long parentId;

    /**
     * 资源被存放的盒子ID，对应表 box 的 ID
     */
    @Column(name = "box_id")
    private Long boxId;

    /**
     * 资源的类型，对应表 resource_type 的 ID
     */
    @Column(name = "type_id")
    private Long typeId;


    /**
     * UFL: 统一文件定位符(Uniform File Locator)，
     * 资源里关于文件的字段名称, 对应文件表的URL
     */
    private String ufl;

    public Long getParentId() {
        return parentId;
    }

    public ResourceEntity setParentId(Long parentId) {
        this.parentId = parentId;
        return this;
    }

    public Long getBoxId() {
        return boxId;
    }

    public ResourceEntity setBoxId(Long boxId) {
        this.boxId = boxId;
        return this;
    }

    public Long getTypeId() {
        return typeId;
    }

    public ResourceEntity setTypeId(Long typeId) {
        this.typeId = typeId;
        return this;
    }

    public String getUfl() {
        return ufl;
    }

    public ResourceEntity setUfl(String ufl) {
        this.ufl = ufl;
        return this;
    }
}
