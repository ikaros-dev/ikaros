package run.ikaros.server.utils;

/**
 * @author li-guohao
 */
public class AssertUtils extends org.springframework.util.Assert {

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
