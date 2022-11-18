package run.ikaros.server.core.service;

import java.util.List;
import javax.annotation.Nonnull;

import org.springframework.transaction.annotation.Transactional;
import run.ikaros.server.entity.OptionEntity;
import run.ikaros.server.enums.OptionCategory;
import run.ikaros.server.model.dto.OptionItemDTO;
import run.ikaros.server.model.request.AppInitRequest;

/**
 * @author li-guohao
 */
public interface OptionService extends CrudService<OptionEntity, Long> {

    @Nonnull
    OptionEntity findOptionItemByKey(@Nonnull String key);

    @Nonnull
    @Transactional
    OptionEntity saveOptionItem(@Nonnull OptionItemDTO optionItemDTO);

    @Transactional
    void deleteOptionItemByKey(@Nonnull String key);

    @Nonnull
    List<OptionEntity> findOptionByCategory(@Nonnull OptionCategory category);

    @Nonnull
    OptionEntity findOptionValueByCategoryAndKey(@Nonnull OptionCategory category,
                                                 @Nonnull String key);

    boolean findAppIsInit();

    /**
     * @return init result msg
     */
    @Transactional
    boolean appInit(@Nonnull AppInitRequest appInitRequest);
}
