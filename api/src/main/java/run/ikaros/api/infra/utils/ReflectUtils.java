package run.ikaros.api.infra.utils;

import java.lang.reflect.Field;
import java.util.Map;

public class ReflectUtils {

    /**
     * 将数据库字段名转换为驼峰命名法的 Java 字段名.
     *
     * @param dbFieldName 数据库字段名（如 ATTACHMENT_ID）
     * @return 转换后的 Java 字段名（如 attachmentId）
     */
    public static String convertToCamelCase(String dbFieldName) {
        if (dbFieldName == null || dbFieldName.isEmpty()) {
            return dbFieldName;
        }

        StringBuilder result = new StringBuilder();
        boolean toUpperCase = false;

        for (int i = 0; i < dbFieldName.length(); i++) {
            char ch = dbFieldName.charAt(i);
            if (ch == '_') {
                toUpperCase = true;
            } else {
                if (toUpperCase) {
                    result.append(Character.toUpperCase(ch));
                    toUpperCase = false;
                } else {
                    result.append(Character.toLowerCase(ch));
                }
            }
        }

        return result.toString();
    }

    /**
     * Map to Object instance.
     */
    public static <T> T mapToClass(Map<String, Object> map, Class<T> clazz,
                                   boolean ignoreMissingField) {
        T instance;
        try {
            instance = clazz.getDeclaredConstructor().newInstance();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String fieldName = convertToCamelCase(entry.getKey());
                Object fieldValue = entry.getValue();

                Field field;
                try {
                    field = clazz.getDeclaredField(fieldName);
                } catch (NoSuchFieldException noSuchFieldException) {
                    if (ignoreMissingField) {
                        continue;
                    } else {
                        throw noSuchFieldException;
                    }
                }
                field.setAccessible(true); // 允许访问私有字段
                field.set(instance, fieldValue);
            }
        }  catch (Exception e) {
            throw new RuntimeException("Mapping failed", e);
        }
        return instance;
    }
}
