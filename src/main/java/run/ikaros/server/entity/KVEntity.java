package run.ikaros.server.entity;

import run.ikaros.server.enums.KVType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity(name = "key_value")
public class KVEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    private KVType type;

    @Column(name = "ik_key")
    private String key;

    @Column(name = "ik_value")
    private String value;

    public KVType getType() {
        return type;
    }

    public KVEntity setType(KVType type) {
        this.type = type;
        return this;
    }

    public String getKey() {
        return key;
    }

    public KVEntity setKey(String key) {
        this.key = key;
        return this;
    }

    public String getValue() {
        return value;
    }

    public KVEntity setValue(String value) {
        this.value = value;
        return this;
    }
}
