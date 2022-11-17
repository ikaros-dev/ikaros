package run.ikaros.server.entity;

import run.ikaros.server.enums.KeyValueType;

import javax.persistence.Entity;

@Entity(name = "key_value")
public class KeyValueEntity extends BaseEntity {

    private KeyValueType type;

    private String key;

    private String value;


    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
