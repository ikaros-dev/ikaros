package run.ikaros.server.service.impl;

import java.lang.reflect.Field;
import java.util.List;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import run.ikaros.server.entity.OptionEntity;
import run.ikaros.server.enums.OptionCategory;
import run.ikaros.server.exceptions.RecordNotFoundException;
import run.ikaros.server.exceptions.ReflectOperateException;
import run.ikaros.server.init.option.PresetOption;
import run.ikaros.server.model.dto.OptionItemDTO;
import run.ikaros.server.repository.OptionRepository;
import run.ikaros.server.service.OptionService;
import run.ikaros.server.service.base.AbstractCrudService;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.JsonUtils;

/**
 * @author guohao
 * @date 2022/10/18
 */
@Service
public class OptionServiceImpl
    extends AbstractCrudService<OptionEntity, Long>
    implements OptionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OptionServiceImpl.class);

    private final OptionRepository optionRepository;

    public OptionServiceImpl(OptionRepository optionRepository) {
        super(optionRepository);
        this.optionRepository = optionRepository;
    }

    @Nonnull
    @Override
    public OptionEntity findOptionItemByKey(@Nonnull String key) {
        AssertUtils.notBlank(key, "'key' must not be blank");
        if (!optionRepository.existsByKeyAndStatus(key, true)) {
            throw new RecordNotFoundException("target key option record not fond, key=" + key);
        }
        return optionRepository.findByKeyAndStatus(key, true);
    }

    @Nonnull
    @Override
    public OptionEntity saveOptionItem(@Nonnull OptionItemDTO optionItemDTO) {
        AssertUtils.notNull(optionItemDTO, "'optionItemDTO' must not be null");
        String key = optionItemDTO.getKey();
        AssertUtils.notBlank(key, "'key' must not be blank");

        try {
            OptionEntity existOptionEntity = findOptionItemByKey(key);
            existOptionEntity.setValue(optionItemDTO.getValue())
                .setCategory(optionItemDTO.getCategory());
            return optionRepository.saveAndFlush(existOptionEntity);
        } catch (RecordNotFoundException e) {
            OptionEntity optionEntity
                = new OptionEntity()
                .setKey(key)
                .setValue(optionItemDTO.getValue())
                .setType(optionItemDTO.getType())
                .setCategory(optionItemDTO.getCategory());
            LOGGER.debug("create new option record: {}", JsonUtils.obj2Json(optionEntity));
            return optionRepository.saveAndFlush(optionEntity);
        }

    }

    @Override
    public void deleteOptionItemByKey(@Nonnull String key) {
        AssertUtils.notBlank(key, "'key' must not be blank");
        OptionEntity optionEntity = findOptionItemByKey(key);
        optionEntity.setStatus(false);
        optionRepository.saveAndFlush(optionEntity);
    }

    @Nonnull
    @Override
    public List<OptionEntity> findOptionByCategory(@Nonnull OptionCategory category) {
        AssertUtils.notNull(category, "category");
        return optionRepository.findByCategoryAndStatus(category, true);
    }

    @Nonnull
    @Override
    public OptionEntity findOptionValueByCategoryAndKey(@Nonnull OptionCategory category,
                                                        @Nonnull String key) {
        AssertUtils.notNull(category, "category");
        AssertUtils.notBlank(key, "key");
        OptionEntity optionEntity =
            optionRepository.findByCategoryAndKeyAndStatus(category, key, true);
        if (optionEntity == null) {
            throw new RecordNotFoundException("target option record not fond, key=" + key
                + " category=" + category);
        }
        return optionEntity;
    }

    @Nonnull
    @Override
    public <T extends PresetOption> T findPresetOption(@Nonnull T presetOption) {
        OptionCategory category = presetOption.getCategory();
        List<OptionEntity> optionEntityList =
            optionRepository.findByCategoryAndStatus(category, true);

        for (Field field : presetOption.getClass().getDeclaredFields()) {
            for (OptionEntity optionEntity : optionEntityList) {
                if (field.getName().equalsIgnoreCase(optionEntity.getKey())) {
                    field.setAccessible(true);
                    try {
                        field.set(presetOption, optionEntity.getValue());
                    } catch (IllegalAccessException e) {
                        throw new ReflectOperateException(e);
                    }
                }
            }
        }

        return presetOption;
    }

}
