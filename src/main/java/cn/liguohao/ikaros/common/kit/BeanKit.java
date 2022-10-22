package cn.liguohao.ikaros.common.kit;

import java.lang.reflect.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author li-guohao
 * @date 2022/06/19
 */
public class BeanKit {
    private static final Logger LOGGER = LoggerFactory.getLogger(BeanKit.class);

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
                if (oldValue == null) {
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
                if (oldValue == null) {
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
}
