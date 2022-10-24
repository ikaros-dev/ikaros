package run.ikaros.server.model.dto;

import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.constants.OptionConst;
import run.ikaros.server.entity.OptionEntity;
import org.springframework.lang.Nullable;

/**
 * @author guohao
 * @date 2022/10/18
 */
public class OptionItemDTO {
    private String key;
    private String value;
    private OptionEntity.Type type = OptionEntity.Type.INTERNAL;

    private String category = OptionConst.Category.DEFAULT;

    public OptionItemDTO(String key, @Nullable String value) {
        AssertUtils.notBlank(key, "'key' must not be blank");
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public OptionItemDTO setKey(String key) {
        AssertUtils.notBlank(key, "'key' must not be blank");
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

    public String getCategory() {
        return category;
    }

    public OptionItemDTO setCategory(String category) {
        this.category = category;
        return this;
    }
}
