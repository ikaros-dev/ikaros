package cn.liguohao.ikaros.service;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.constants.OptionConstants;
import cn.liguohao.ikaros.exceptions.NotSupportException;
import cn.liguohao.ikaros.exceptions.RecordNotFoundException;
import cn.liguohao.ikaros.model.dto.OptionItemDTO;
import cn.liguohao.ikaros.model.entity.OptionEntity;
import cn.liguohao.ikaros.repository.OptionRepository;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.transaction.Transactional;
import org.springframework.stereotype.Service;

/**
 * @author guohao
 * @date 2022/10/18
 */
@Service
@Transactional(rollbackOn = Exception.class)
public class OptionService {

    private final OptionRepository optionRepository;

    public OptionService(OptionRepository optionRepository) {
        this.optionRepository = optionRepository;
    }

    public OptionEntity findOptionItemByKey(String key) throws RecordNotFoundException {
        Assert.notBlank(key, "'key' must not be blank");
        if (!optionRepository.existsByKeyAndStatus(key, true)) {
            throw new RecordNotFoundException("target key option record not fond, key=" + key);
        }
        return optionRepository.findByKeyAndStatus(key, true);
    }

    public OptionEntity saveOptionItem(OptionItemDTO optionItemDTO) {
        Assert.notNull(optionItemDTO, "'optionItemDTO' must not be null");
        String key = optionItemDTO.getKey();
        Assert.notBlank(key, "'key' must not be blank");

        try {
            OptionEntity existOptionEntity = findOptionItemByKey(key);
            existOptionEntity.setValue(optionItemDTO.getValue())
                .setCategory(optionItemDTO.getCategory());
            return optionRepository.saveAndFlush(existOptionEntity);
        } catch (RecordNotFoundException e) {
            OptionEntity optionEntity
                = new OptionEntity(key, optionItemDTO.getValue())
                .setType(optionItemDTO.getType())
                .setCategory(optionItemDTO.getCategory());
            return optionRepository.saveAndFlush(optionEntity);
        }

    }

    public void deleteOptionItemByKey(String key) throws RecordNotFoundException {
        Assert.notBlank(key, "'key' must not be blank");
        OptionEntity optionEntity = findOptionItemByKey(key);
        optionEntity.setStatus(false);
        optionRepository.saveAndFlush(optionEntity);
    }

    public List<OptionEntity> findOptionByCategory(String category) {
        Assert.notBlank(category, "'category' must not be blank");
        return optionRepository.findByCategoryAndStatus(category, true);
    }

    public OptionEntity findOptionValueByCategoryAndKey(String category, String key)
        throws RecordNotFoundException {
        Assert.notBlank(category, "'category' must not be blank");
        Assert.notBlank(key, "'key' must not be blank");
        OptionEntity optionEntity =
            optionRepository.findByCategoryAndKeyAndStatus(category, key, true);
        if (optionEntity == null) {
            throw new RecordNotFoundException("target option record not fond, key=" + key
                + " category=" + category);
        }
        return optionEntity;
    }

    /**
     * init all preset options
     *
     * @see OptionConstants.Category
     * @see OptionConstants.Init
     */
    public void initPresetOptionItems() throws NotSupportException, IllegalAccessException {
        boolean needInitOptionItems = false;

        // search init result
        try {
            OptionEntity appIsInitOptionEntity =
                findOptionValueByCategoryAndKey(OptionConstants.Category.APP,
                    OptionConstants.Init.App.IS_INIT[0]);

            String value = appIsInitOptionEntity.getValue();

            if ("false".equalsIgnoreCase(value)) {
                needInitOptionItems = true;
            }

        } catch (RecordNotFoundException e) {
            needInitOptionItems = true;
        }

        if (!needInitOptionItems) {
            return;
        }

        // init all preset options
        Set<String> categorySet = Arrays.stream(OptionConstants.Category.class.getDeclaredFields())
            .filter(field -> field.getType() == String.class)
            .flatMap((Function<Field, Stream<String>>) field
                -> Stream.of(field.getName()))
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

        for (Class<?> cls : OptionConstants.Init.class.getDeclaredClasses()) {
            String category = cls.getSimpleName().toLowerCase();
            for (Field field : cls.getDeclaredFields()) {
                if (field.getType() != String[].class) {
                    throw new NotSupportException("Please set it in the correct format, "
                        + "format: 'String[] example = {\"key\", \"value\"}'");
                }

                if (!categorySet.contains(category)) {
                    throw new NotSupportException(
                        "Current category= " + category + " not set in OptionConstants.Category, "
                            + "please add a field in OptionConstants.Category.");
                }

                field.setAccessible(true);
                String[] optionItemStrArr = (String[]) field.get(null);

                OptionItemDTO optionItemDTO =
                    new OptionItemDTO(optionItemStrArr[0], optionItemStrArr[1])
                        .setCategory(category);

                saveOptionItem(optionItemDTO);
            }
        }
    }

}
