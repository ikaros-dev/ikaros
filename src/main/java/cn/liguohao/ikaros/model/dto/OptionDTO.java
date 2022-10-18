package cn.liguohao.ikaros.model.dto;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.kit.BeanKit;
import cn.liguohao.ikaros.model.entity.OptionEntity;

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
        Assert.notNull(optionEntity, "'optionEntity' must not be null");
        BeanKit.copyProperties(optionEntity, this);
    }

    public Integer getTabKey() {
        return tabKey;
    }

    public OptionDTO setTabKey(Integer tabKey) {
        this.tabKey = tabKey;
        return this;
    }
}
