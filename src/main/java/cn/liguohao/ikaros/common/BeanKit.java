package cn.liguohao.ikaros.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

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
        for (Field declaredField : sourceClass.getDeclaredFields()) {
            try {
                declaredField.setAccessible(true);
                Object oldValue = declaredField.get(source);
                declaredField.set(target, oldValue);
                declaredField.setAccessible(false);
            } catch (IllegalAccessException e) {
                String msg = "copy field fail, fileName=" + declaredField.getName();
                LOGGER.error(msg, e);
            }
        }

    }

    /**
     * 从原来到对象，复制相同字段的值，到新到对象，要求字段类型和名称相同，可用于不同类型到对象。
     *
     * @param source 源头对象
     * @param dest   目标对象
     * @param <S>    源头对象类型
     * @param <D>    目标对象类型
     */
    public static <S, D> void copyFieldValue(S source, D dest) throws IllegalAccessException {
        Assert.isNotNull(source, dest);

        Class<?> sourceCls = source.getClass();
        Class<?> destCls = dest.getClass();

        // 复制相同类型相同名称的字段值，从源头对象到目标对象
        for (Field sourceField : sourceCls.getDeclaredFields()) {
            for (Field destField : destCls.getDeclaredFields()) {
                if (sourceField.getName().equals(destField.getName())
                        && sourceField.getType().equals(destField.getType())) {
                    boolean sourceCanAccess = sourceField.canAccess(source);
                    boolean destCanAccess = destField.canAccess(dest);

                    try {
                        sourceField.setAccessible(true);
                        destField.setAccessible(true);
                        Object value = sourceField.get(source);
                        destField.set(dest, value);
                    } finally {
                        sourceField.setAccessible(sourceCanAccess);
                        destField.setAccessible(destCanAccess);
                    }

                }
            }
        }
    }
}
