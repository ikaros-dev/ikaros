package cn.liguohao.ikaros.model.entity;

import cn.liguohao.ikaros.common.constants.InitConstants;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "resource", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"type_id", "name"})})
public class ResourceEntity extends BaseEntity {

    /**
     * 资源被存放的盒子ID，对应表 box 的 ID
     */
    @Column(name = "box_id")
    private Long boxId;

    /**
     * 资源的类型，对应表 resource_type 的 ID
     */
    @Column(name = "type_id", nullable = false)
    private Long typeId = InitConstants.ROOT_ID;

    /**
     * 资源的名称，如果是文件类型的资源，应该带有后缀名
     */
    @Column(nullable = false)
    private Long name;


    /**
     * UFL: 统一文件定位符(Uniform File Locator)，
     * 资源里关于文件的字段名称, 对应文件表的URL
     */
    private String ufl;

    public Long getName() {
        return name;
    }

    public ResourceEntity setName(Long name) {
        this.name = name;
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
