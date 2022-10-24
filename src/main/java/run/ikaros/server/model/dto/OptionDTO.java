package run.ikaros.server.model.dto;

import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.BeanUtils;
import run.ikaros.server.entity.OptionEntity;

/**
 * @author guohao
 * @date 2022/10/19
 */
public class OptionDTO extends OptionEntity {

    /**
     * 这个是在前端界面的Tab页的key
     */
    private Integer tabKey;

    public OptionDTO() {
    }

    public OptionDTO(String key, String value) {
        super(key, value);
    }

    public OptionDTO(OptionEntity optionEntity) {
        AssertUtils.notNull(optionEntity, "'optionEntity' must not be null");
        BeanUtils.copyProperties(optionEntity, this);
    }

    public Integer getTabKey() {
        return tabKey;
    }

    public OptionDTO setTabKey(Integer tabKey) {
        this.tabKey = tabKey;
        return this;
    }
}
