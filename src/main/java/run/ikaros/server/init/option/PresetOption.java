package run.ikaros.server.init.option;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import run.ikaros.server.entity.OptionEntity;
import run.ikaros.server.enums.OptionCategory;
import run.ikaros.server.exceptions.PresetOptionIllegalException;
import run.ikaros.server.exceptions.ReflectOperateException;
import run.ikaros.server.utils.AssertUtils;

/**
 * @author li-guohao
 */
public interface PresetOption {
    @Nonnull
    OptionCategory getCategory();

    @Nonnull
    static <T extends PresetOption> List<OptionEntity> buildEntityListByPresetOption(
        @Nonnull T presetOption) {
        AssertUtils.notNull(presetOption, "presetOption");

        List<OptionEntity> optionEntityList = new ArrayList<>();
        Class<? extends PresetOption> cls = presetOption.getClass();
        for (Field field : cls.getDeclaredFields()) {
            if (field.getType() != String.class) {
                throw new PresetOptionIllegalException(
                    "preset option field must be String type, current type=" + field.getType());
            }
            try {
                OptionCategory category = presetOption.getCategory();
                String key = field.getName();
                field.setAccessible(true);
                String value = (String) field.get(presetOption);
                optionEntityList.add(new OptionEntity(key, value).setCategory(category));
            } catch (IllegalAccessException e) {
                throw new ReflectOperateException(
                    "build entity list by preset option fail, get field value fail", e);
            }
        }
        return optionEntityList;
    }

    @Nonnull
    @SuppressWarnings("deprecation")
    static <T extends PresetOption> T buildByEntityList(
        @Nonnull List<OptionEntity> optionEntityList,
        @Nonnull Class<T> clazz) {
        AssertUtils.notNull(optionEntityList, "optionEntityList");
        AssertUtils.notNull(clazz, "clazz");

        try {
            T presetOption = clazz.newInstance();

            for (OptionEntity optionEntity : optionEntityList) {
                // category matching
                if (optionEntity.getCategory() == presetOption.getCategory()) {
                    for (Field field : clazz.getDeclaredFields()) {
                        String key = field.getName();
                        // key matching
                        if (key.equalsIgnoreCase(optionEntity.getKey())) {
                            if (optionEntity.getValue() != null) {
                                field.setAccessible(true);
                                field.set(presetOption, optionEntity.getValue());
                            }
                        }
                    }
                }
            }
            return presetOption;
        } catch (ReflectiveOperationException e) {
            throw new ReflectOperateException(
                "build preset option by entity fail, set field fail", e);
        }
    }
}
