package run.ikaros.server.entity;

import run.ikaros.server.constants.OptionConst;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * Setting entity.
 *
 * @author johnniang
 * @author ryanwang
 * @date 2019-03-20
 */
@Entity
@Table(name = "options")
public class OptionEntity extends BaseEntity {

    public enum Type {
        INTERNAL,

        CUSTOM;
    }

    private Type type = OptionConst.Type.DEFAULT;

    @Column(name = "ikkey", nullable = false)
    private String key = "";

    /**
     * option value
     */
    @Column(name = "ikvalue", nullable = false)
    @Lob
    private String value  = "";

    @Column(nullable = false)
    private String category = OptionConst.Category.DEFAULT;

    public OptionEntity() {
    }

    public OptionEntity(String key, String value) {
        this.key = key;
        this.value = value;
    }


    public Type getType() {
        return type;
    }

    public OptionEntity setType(Type type) {
        this.type = type;
        return this;
    }

    public String getKey() {
        return key;
    }

    public OptionEntity setKey(String key) {
        this.key = key;
        return this;
    }

    public String getValue() {
        return value;
    }

    public OptionEntity setValue(String value) {
        this.value = value;
        return this;
    }

    public String getCategory() {
        return category;
    }

    public OptionEntity setCategory(String category) {
        this.category = category;
        return this;
    }
}
