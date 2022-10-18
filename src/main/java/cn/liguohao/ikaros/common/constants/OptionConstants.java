package cn.liguohao.ikaros.common.constants;

import cn.liguohao.ikaros.model.entity.OptionEntity;

/**
 * @author guohao
 * @date 2022/10/18
 */
public interface OptionConstants {


    interface Category {
        String DEFAULT = "default";
        String SYSTEM_COMMON = "system_common";
    }

    interface Type {
        OptionEntity.Type DEFAULT = OptionEntity.Type.INTERNAL;
    }


}
