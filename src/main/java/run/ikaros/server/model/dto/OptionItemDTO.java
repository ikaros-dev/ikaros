package run.ikaros.server.model.dto;

import run.ikaros.server.enums.OptionCategory;
import run.ikaros.server.enums.OptionType;
import run.ikaros.server.utils.AssertUtils;
import org.springframework.lang.Nullable;

/**
 * @author guohao
 * @date 2022/10/18
 */
public class OptionItemDTO {
    private String key;
    private String value;
    private OptionType type = OptionType.INTERNAL;

    private OptionCategory category = OptionCategory.OTHER;

    public OptionItemDTO(String key, @Nullable String value) {
        AssertUtils.notBlank(key, "'key' must not be blank");
        this.key = key;
        this.value = value;
    }

    public OptionItemDTO(String key, String value, OptionCategory category) {
        this.key = key;
        this.value = value;
        this.category = category;
    }

    public String getKey() {
        return key;
    }

    public OptionItemDTO setKey(String key) {
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

    public OptionType getType() {
        return type;
    }

    public OptionItemDTO setType(OptionType type) {
        this.type = type;
        return this;
    }

    public OptionCategory getCategory() {
        return category;
    }

    public OptionItemDTO setCategory(OptionCategory category) {
        this.category = category;
        return this;
    }
}
