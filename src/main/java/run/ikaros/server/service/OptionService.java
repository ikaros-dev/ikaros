package run.ikaros.server.service;

import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.JsonUtils;
import run.ikaros.server.constants.OptionConst;
import run.ikaros.server.constants.OptionConst.Category;
import run.ikaros.server.constants.OptionConst.Init.App;
import run.ikaros.server.utils.ClassUtils;
import run.ikaros.server.model.option.OptionModel;
import run.ikaros.server.exceptions.NotSupportException;
import run.ikaros.server.exceptions.RecordNotFoundException;
import run.ikaros.server.model.dto.OptionItemDTO;
import run.ikaros.server.entity.OptionEntity;
import run.ikaros.server.repository.OptionRepository;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author guohao
 * @date 2022/10/18
 */
@Service
@Transactional(rollbackOn = Exception.class)
public class OptionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OptionService.class);

    private final OptionRepository optionRepository;

    public OptionService(OptionRepository optionRepository) {
        this.optionRepository = optionRepository;
    }

    public OptionEntity findOptionItemByKey(String key) throws RecordNotFoundException {
        AssertUtils.notBlank(key, "'key' must not be blank");
        if (!optionRepository.existsByKeyAndStatus(key, true)) {
            throw new RecordNotFoundException("target key option record not fond, key=" + key);
        }
        return optionRepository.findByKeyAndStatus(key, true);
    }

    public OptionEntity saveOptionItem(OptionItemDTO optionItemDTO) {
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
                = new OptionEntity(key, optionItemDTO.getValue())
                .setType(optionItemDTO.getType())
                .setCategory(optionItemDTO.getCategory());
            LOGGER.debug("create new option record: {}", JsonUtils.obj2Json(optionEntity));
            return optionRepository.saveAndFlush(optionEntity);
        }

    }

    public void deleteOptionItemByKey(String key) throws RecordNotFoundException {
        AssertUtils.notBlank(key, "'key' must not be blank");
        OptionEntity optionEntity = findOptionItemByKey(key);
        optionEntity.setStatus(false);
        optionRepository.saveAndFlush(optionEntity);
    }

    public List<OptionEntity> findOptionByCategory(String category) {
        AssertUtils.notBlank(category, "'category' must not be blank");
        return optionRepository.findByCategoryAndStatus(category, true);
    }

    public OptionEntity findOptionValueByCategoryAndKey(String category, String key)
        throws RecordNotFoundException {
        AssertUtils.notBlank(category, "'category' must not be blank");
        AssertUtils.notBlank(key, "'key' must not be blank");
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
     * @see Category
     * @see OptionConst.Init
     */
    public void initPresetOptionItems() throws NotSupportException, IllegalAccessException {
        boolean needInitOptionItems = false;

        // search init result
        try {
            OptionEntity appIsInitOptionEntity =
                findOptionValueByCategoryAndKey(Category.APP, App.IS_INIT[0]);

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
        Set<String> categorySet = Arrays.stream(Category.class.getDeclaredFields())
            .filter(field -> field.getType() == String.class)
            .flatMap((Function<Field, Stream<String>>) field
                -> Stream.of(field.getName()))
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

        for (Class<?> cls : OptionConst.Init.class.getDeclaredClasses()) {
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

    @SuppressWarnings("deprecation")
    public List<OptionModel> findAllOptionModel()
        throws InstantiationException, IllegalAccessException, NoSuchFieldException, IOException {
        List<OptionModel> optionModels = new ArrayList<>();

        List<Class<?>> optionModelClsList =
            ClassUtils.findClassByPackage(OptionConst.MODEL_CLASS_PACKAGE_NAME);
        for (Class<?> cls : optionModelClsList) {
            // todo 这里如果不是 OptionModel 接口实现类 强转会报错， 先这样后面再改
            OptionModel instance = (OptionModel) cls.newInstance();
            Field categoryField = cls.getDeclaredField("category");
            categoryField.setAccessible(true);
            if (categoryField.getType() != String.class) {
                continue;
            }
            String category = (String) categoryField.get(instance);

            Map<String, String> keyValueMap = new HashMap<>();
            optionRepository
                .findByCategoryAndStatus(category, true)
                .forEach(optionEntity -> keyValueMap.put(optionEntity.getKey(),
                    optionEntity.getValue()));

            for (Field field : cls.getDeclaredFields()) {
                field.setAccessible(true);
                String fieldName = field.getName();
                if (keyValueMap.containsKey(fieldName)) {
                    field.set(instance, keyValueMap.get(fieldName));
                }
            }

            optionModels.add(instance);
        }

        return optionModels;
    }

    public void saveOptionModel(OptionModel optionModel) throws IllegalAccessException {
        AssertUtils.notNull(optionModel, "'optionModel' must not be null");
        String category = optionModel.getCategory();
        for (Field field : optionModel.getClass().getDeclaredFields()) {
            String fieldName = field.getName();
            if ("category".equalsIgnoreCase(fieldName)) {
                continue;
            }
            field.setAccessible(true);
            String fieldValue = (String) field.get(optionModel);
            saveOptionItem(new OptionItemDTO(fieldName, fieldValue).setCategory(category));
        }
    }

    @SuppressWarnings({"deprecation", "unchecked"})
    public <T> T findOptionModel(T optionModel) {
        AssertUtils.notNull(optionModel, "'optionModel' must not be null");
        AssertUtils.isTrue(optionModel instanceof OptionModel,
            "'optionModel' must implements OptionModel interface");

        String category = ((OptionModel) optionModel).getCategory();
        AssertUtils.notBlank(category, "'optionModel' category must not be blank");

        Class<T> cls = (Class<T>) optionModel.getClass();

        T instance = null;
        try {
            instance = cls.newInstance();

            for (OptionEntity optionEntity : findOptionByCategory(category)) {
                String fieldName = optionEntity.getKey();
                String fieldValue = optionEntity.getValue();
                for (Field field : cls.getDeclaredFields()) {
                    if (field.getName().equalsIgnoreCase(fieldName)) {
                        field.setAccessible(true);
                        field.set(instance, fieldValue);
                    }
                }
            }
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("find category={} OptionModel fail", category, exception);
        }

        return instance;
    }
}
