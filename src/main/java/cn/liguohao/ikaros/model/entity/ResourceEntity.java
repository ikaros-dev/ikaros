package cn.liguohao.ikaros.model.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "resource", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"type", "name"})})
public class ResourceEntity extends BaseEntity {
    public enum Type {
        IMAGE(1),
        DOCUMENT(2),
        VIDEO(3),
        VOICE(4),
        FILE(99);

        private final int code;

        Type(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

    }

    /**
     * 资源被存放的盒子ID，对应表 box 的 ID
     */
    @Column(name = "box_id")
    private Long boxId;

    private Integer type = Type.FILE.getCode();

    /**
     * 资源的名称，如果是文件类型的资源，应该带有后缀名
     */
    @Column(nullable = false)
    private String name;


    private String url;

    public String getName() {
        return name;
    }

    public ResourceEntity setName(String name) {
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

    public Integer getType() {
        return type;
    }

    public ResourceEntity setType(Integer type) {
        this.type = type;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public ResourceEntity setUrl(String url) {
        this.url = url;
        return this;
    }
}
