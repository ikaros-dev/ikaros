package cn.liguohao.ikaros.common;

import java.util.Objects;

/**
 * @author li-guohao
 */
public class Assert extends org.springframework.util.Assert {

    /**
     * 校验参数不为null
     *
     * @param objs 待校验的对象
     */
    public static void isNotNull(Object... objs) {
        for (Object obj : objs) {
            if (Objects.isNull(obj)) {
                throw new IllegalArgumentException("obj is null.");
            }
        }
    }

    /**
     * 校验字符串参数不为空。
     *
     * @param strings 待校验的参数，支持多个字符串
     */
    public static void isNotBlank(String... strings) {
        for (String str : strings) {
            if (Strings.isBlank(str)) {
                throw new IllegalArgumentException("str is blank.");
            }
        }
    }

    /**
     * 校验id是否符合规定，id大部分预期为正整数，也可为0或者-1，不可为其它负数。
     *
     * @param id 待校验的ID
     */
    public static void checkId(Long id) {
        isNotNull(id);

        isPositive(id);
    }

    /**
     * 校验是正数
     *
     * @param numbers 待校验的数
     */
    public static void isPositive(int... numbers) {
        for (int number : numbers) {
            isNotNull(number);
            if (number < 0) {
                throw new IllegalArgumentException("number is not positive. number=" + number);
            }
        }
    }

    /**
     * 校验是正数
     *
     * @param numbers 待校验的数
     */
    public static void isPositive(long... numbers) {
        for (long number : numbers) {
            isNotNull(number);
            if (number < 0) {
                throw new IllegalArgumentException("number is not positive. number=" + number);
            }
        }
    }


}
