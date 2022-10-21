package cn.liguohao.ikaros.model.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "metadata", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"type", "host_id", "name"})})
public class MetadataEntity extends BaseEntity {
    public enum Type {
        BOX(1),
        RESOURCE(2);

        private final int code;

        Type(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

    }

    /**
     * <ul>
     *     <li>当type为box时，代表 box_id</li>
     *     <li>当type为resource时，代表 resource_id</li>
     * </ul>
     *
     * @see Type
     */
    @Column(name = "host_id")
    private Long hostId;
    /**
     * @see Type
     */
    private Integer type;

    private String name;
    private String value;

    public Long getHostId() {
        return hostId;
    }

    public MetadataEntity setHostId(Long hostId) {
        this.hostId = hostId;
        return this;
    }

    public Integer getType() {
        return type;
    }

    public MetadataEntity setType(Integer type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public MetadataEntity setName(String name) {
        this.name = name;
        return this;
    }

    public String getValue() {
        return value;
    }

    public MetadataEntity setValue(String value) {
        this.value = value;
        return this;
    }
}
