package run.ikaros.server.service;

import java.util.List;
import javax.annotation.Nonnull;
import javax.transaction.Transactional;
import run.ikaros.server.entity.OptionEntity;
import run.ikaros.server.enums.OptionCategory;
import run.ikaros.server.init.option.PresetOption;
import run.ikaros.server.init.option.ThirdPartyPresetOption;
import run.ikaros.server.model.dto.OptionItemDTO;
import run.ikaros.server.service.base.CrudService;

/**
 * @author li-guohao
 */
public interface OptionService extends CrudService<OptionEntity, Long> {

    @Nonnull
    OptionEntity findOptionItemByKey(@Nonnull String key);

    @Nonnull
    @Transactional(rollbackOn = Exception.class)
    OptionEntity saveOptionItem(@Nonnull OptionItemDTO optionItemDTO);

    @Transactional(rollbackOn = Exception.class)
    void deleteOptionItemByKey(@Nonnull String key);

    @Nonnull
    List<OptionEntity> findOptionByCategory(@Nonnull OptionCategory category);

    @Nonnull
    OptionEntity findOptionValueByCategoryAndKey(@Nonnull OptionCategory category,
                                                 @Nonnull String key);

    @Nonnull
    <T extends PresetOption> T findPresetOption(@Nonnull T presetOption);
}
