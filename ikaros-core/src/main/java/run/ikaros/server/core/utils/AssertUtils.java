package run.ikaros.server.core.utils;

import run.ikaros.server.core.exceptions.IllegalArgumentRuntimeIkarosException;

/**
 * @author li-guohao
 */
public class AssertUtils {

    public static void notNull(Object obj, String message) {
        if (obj == null) {
            throw new IllegalArgumentRuntimeIkarosException(message);
        }
    }

    public static void notBlank(String str, String message) {
        notNull(str, "'str' must not be null" + " | " + message);
        if (StringUtils.isBlank(str)) {
            throw new IllegalArgumentException(message);
        }
    }


    public static void isPositive(long number, String message) {
        if (number < 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
