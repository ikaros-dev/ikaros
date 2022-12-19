package run.ikaros.server.entity;

import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;
import org.hibernate.usertype.UserType;
import run.ikaros.server.enums.OptionCategory;
import run.ikaros.server.enums.OptionType;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.sql.Types;

import static jakarta.persistence.FetchType.LAZY;

/**
 * @author li-guohao
 */
@Entity
@Table(name = "options")
public class OptionEntity extends BaseEntity {

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OptionType type = OptionType.INTERNAL;

    @Column(name = "o_key", nullable = false)
    private String key = "";

    @Basic(fetch = LAZY)
    @Column(name = "o_value", length = 50000)
    private String value = "";

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OptionCategory category = OptionCategory.OTHER;

    public OptionEntity() {
    }

    public OptionEntity(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public OptionType getType() {
        return type;
    }

    public OptionEntity setType(OptionType type) {
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

    public OptionCategory getCategory() {
        return category;
    }

    public OptionEntity setCategory(OptionCategory category) {
        this.category = category;
        return this;
    }
}
