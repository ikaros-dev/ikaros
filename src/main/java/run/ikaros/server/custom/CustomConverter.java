package run.ikaros.server.custom;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import run.ikaros.server.infra.exception.CustomConvertException;
import run.ikaros.server.store.entity.CustomEntity;
import run.ikaros.server.store.entity.CustomMetadataEntity;

@Component
public class CustomConverter {

    private static final ObjectMapper objectMapper = JsonMapper.builder()
        .addModule(new JavaTimeModule())
        .build();

    /**
     * convert Custom to CustomDto.
     *
     * @param custom a Custom
     * @param <C>    Custom type
     * @return a CustomDto
     */
    public static <C> CustomDto convertTo(C custom) {
        Assert.notNull(custom, "'custom' must not null");
        Class<?> cls = custom.getClass();
        Custom customAnnotation = cls.getAnnotation(Custom.class);
        Assert.notNull(customAnnotation,
            "class must annotation by @run.ikaros.server.custom.Custom");

        String name = getNameFieldValue(custom);

        CustomEntity customEntity = CustomEntity.builder()
            .group(customAnnotation.group())
            .version(customAnnotation.version())
            .kind(customAnnotation.kind())
            .name(name)
            .build();

        List<CustomMetadataEntity> customMetadataEntities = new ArrayList<>();
        for (Field field : Arrays.stream(cls.getDeclaredFields())
            .filter(field -> !field.isAnnotationPresent(Name.class))
            .toList()) {
            String fieldName = field.getName();
            try {
                if (field.trySetAccessible()) {
                    Object fieldValue = field.get(custom);
                    customMetadataEntities.add(CustomMetadataEntity.builder()
                        .key(fieldName)
                        .value(objectMapper.writeValueAsBytes(fieldValue))
                        .build());
                } else {
                    throw new CustomConvertException("set field accessible fail for for class: "
                        + cls + " and field name:" + fieldName);
                }
            } catch (IllegalAccessException e) {
                throw new CustomConvertException(
                    "get field value fail for class: " + cls + " and field name:" + fieldName, e);
            } catch (JsonProcessingException e) {
                throw new CustomConvertException(
                    "convert field value to bytes fail for class: " + cls + " and field name:"
                        + fieldName, e);
            }
        }

        return new CustomDto(customEntity, customMetadataEntities);
    }

    /**
     * get current custom instance @Name field value.
     *
     * @param custom a custom instance
     * @param <C>    custom instance class type
     * @return current custom instance @Name field value
     */
    public static <C> String getNameFieldValue(C custom) {
        if (custom == null) {
            return null;
        }
        Field nameField = getNameField(custom.getClass());
        String name;
        try {
            nameField.setAccessible(true);
            name = (String) nameField.get(custom);
        } catch (IllegalAccessException e) {
            throw new CustomConvertException(
                "get custom name filed value fail for name=" + nameField.getName(), e);
        }
        if (!StringUtils.hasText(name)) {
            throw new CustomConvertException(
                "get custom name filed value fail for name=" + nameField.getName());
        }
        return name;
    }

    private static Field getNameField(Class<?> cls) {
        List<Field> nameFields = Arrays.stream(cls.getDeclaredFields())
            .filter(field -> field.isAnnotationPresent(Name.class))
            .filter(field -> String.class.equals(field.getType()))
            .toList();
        if (nameFields.size() != 1) {
            throw new IllegalArgumentException(
                "@run.ikaros.server.custom.Custom mark class "
                    + "must has one field that type is String and"
                    + " mark by @run.ikaros.server.custom.Name");
        }
        return nameFields.get(0);
    }

    /**
     * covert CustomDto to Custom.
     *
     * @param customType custom type
     * @param customDto  a CustomDto
     * @param <C>        custom type
     * @return a Custom
     */
    public static <C> C convertFrom(Class<C> customType, CustomDto customDto) {
        Assert.notNull(customType, "'customType' must not null");
        Assert.notNull(customDto, "'customDto' must not null");

        CustomEntity customEntity = customDto.customEntity();
        Assert.notNull(customEntity, "'customDto.customEntity' must not null");
        final String name = customEntity.getName();
        Assert.hasText(name, "'customEntity.name' must has text");
        Field nameField = getNameField(customType);

        C custom;
        try {
            custom = customType.getDeclaredConstructor().newInstance();
            if (nameField.trySetAccessible()) {
                nameField.set(custom, name);
            } else {
                throw new CustomConvertException(
                    "set custom @Name field value fail for custom class="
                        + customType);
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                 | NoSuchMethodException e) {
            throw new CustomConvertException(
                "new Instance or set @Name field value fail for Custom class:"
                    + customType, e);
        }


        List<CustomMetadataEntity> customMetadataEntityList = customDto.customMetadataEntityList();
        HashMap<String, byte[]> metadataMap = new HashMap<>();
        if (customMetadataEntityList != null && !customMetadataEntityList.isEmpty()) {
            for (CustomMetadataEntity customMetadataEntity : customMetadataEntityList) {
                if (customEntity.getId() != null
                    && !customEntity.getId().equals(customMetadataEntity.getCustomId())) {
                    continue;
                }
                metadataMap.put(customMetadataEntity.getKey(),
                    customMetadataEntity.getValue());
            }
        }

        if (metadataMap.isEmpty()) {
            return custom;
        }

        for (Field field : Arrays.stream(customType.getDeclaredFields())
            .filter(field -> !field.isAnnotationPresent(Name.class))
            .toList()) {
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();
            if (metadataMap.containsKey(fieldName)) {
                byte[] bytes = metadataMap.get(fieldName);
                try {
                    Object fieldValue = objectMapper.readerFor(fieldType).readValue(bytes);
                    if (Modifier.isStatic(field.getModifiers())
                        || Modifier.isFinal(field.getModifiers())) {
                        // skip static and final field
                        continue;
                    }
                    if (field.trySetAccessible()) {
                        field.set(custom, fieldValue);
                    } else {
                        throw new CustomConvertException(
                            "set custom field value fail for class="
                                + customType + ". field=" + fieldName);
                    }
                } catch (IOException | IllegalAccessException e) {
                    throw new CustomConvertException(
                        "read custom field value fail form metadata entity value for class="
                            + customType + ". field=" + fieldName, e);
                }

            }
        }

        return custom;
    }
}
