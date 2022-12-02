package run.ikaros.server.core.service;

import java.util.List;
import javax.annotation.Nonnull;

import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;
import run.ikaros.server.entity.OptionEntity;
import run.ikaros.server.enums.OptionCategory;
import run.ikaros.server.model.dto.OptionDTO;
import run.ikaros.server.model.dto.OptionItemDTO;
import run.ikaros.server.model.dto.OptionNetworkDTO;
import run.ikaros.server.model.dto.OptionQbittorrentDTO;
import run.ikaros.server.model.request.AppInitRequest;
import run.ikaros.server.model.request.SaveOptionRequest;

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

    @Nullable
    OptionEntity findOptionValueByCategoryAndKey(@Nonnull OptionCategory category,
                                                 @Nonnull String key);

    boolean findAppIsInit();

    boolean appInit(@Nonnull AppInitRequest appInitRequest);

    /**
     * @param isCoerce 是否强制初始化
     * @return init result msg
     */
    @Transactional
    boolean appInit(@Nonnull AppInitRequest appInitRequest, boolean isCoerce);

    @Nonnull
    List<OptionDTO> findOptions(@Nullable String category);

    @Nonnull
    @Transactional
    List<OptionDTO> saveWithRequest(@Nonnull SaveOptionRequest saveOptionRequest);

    @Nonnull
    OptionNetworkDTO getOptionNetworkDTO();

    @Nonnull
    OptionQbittorrentDTO getOptionQbittorrentDTO();
}
