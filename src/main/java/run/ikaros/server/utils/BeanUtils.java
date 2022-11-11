package run.ikaros.server.utils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author li-guohao
 * @date 2022/06/19
 */
public class BeanUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(BeanUtils.class);

    /**
     * 复制相同类型对象的相同类型属性值
     *
     * @param source 源对象
     * @param target 目标对象
     */
    public static <T> void copyProperties(T source, T target) {
        Class<?> sourceClass = source.getClass();
        // copy current class declared fields
        for (Field declaredField : sourceClass.getDeclaredFields()) {
            try {
                declaredField.setAccessible(true);
                Object oldValue = declaredField.get(source);
                Object oldTargetValue = declaredField.get(target);
                if (oldValue == null || oldValue.equals(oldTargetValue)) {
                    continue;
                }
                declaredField.set(target, oldValue);
                declaredField.setAccessible(false);
            } catch (IllegalAccessException e) {
                String msg = "copy field fail, fileName=" + declaredField.getName();
                LOGGER.error(msg, e);
            }
        }

        // copy parent class declared fields
        Class<?> superclass = sourceClass.getSuperclass();
        for (Field declaredField : superclass.getDeclaredFields()) {
            try {
                declaredField.setAccessible(true);
                Object oldValue = declaredField.get(source);
                Object oldTargetValue = declaredField.get(target);
                if (oldValue == null || oldValue.equals(oldTargetValue)) {
                    continue;
                }
                declaredField.set(target, oldValue);
                declaredField.setAccessible(false);
            } catch (IllegalAccessException e) {
                String msg = "copy field fail, fileName=" + declaredField.getName();
                LOGGER.error(msg, e);
            }
        }

        if (superclass.getSuperclass() != Object.class) {
            throw new IllegalArgumentException(
                "current kit method must can support two extend relation");
        }
    }



    @Nonnull
    public static <T> T map2Bean(@Nonnull Map<String, Object> map,
                                 @Nonnull Class<T> resultCls) {
        AssertUtils.notNull(map, "map");
        AssertUtils.isFalse(map.isEmpty(), "map is empty");
        AssertUtils.notNull(resultCls, "resultCls");

        String json = JsonUtils.obj2Json(map);
        return Objects.requireNonNull(JsonUtils.json2obj(json, resultCls));
    }
}
