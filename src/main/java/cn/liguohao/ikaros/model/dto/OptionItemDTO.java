package cn.liguohao.ikaros.model.dto;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.model.entity.OptionEntity;
import org.springframework.lang.Nullable;

/**
 * @author guohao
 * @date 2022/10/18
 */
public class OptionItemDTO {
    private String key;
    private String value;
    private OptionEntity.Type type = OptionEntity.Type.INTERNAL;

    public OptionItemDTO(String key, @Nullable String value) {
        Assert.notBlank(key, "'key' must not be blank");
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public OptionItemDTO setKey(String key) {
        Assert.notBlank(key, "'key' must not be blank");
        this.key = key;
        return this;
    }

    public String getValue() {
        return value;
    }

    public OptionItemDTO setValue(String value) {
        this.value = value;
        return this;
    }

    public OptionEntity.Type getType() {
        return type;
    }

    public OptionItemDTO setType(OptionEntity.Type type) {
        this.type = type;
        return this;
    }
}
