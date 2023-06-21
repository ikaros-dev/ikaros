package run.ikaros.server.infra.utils;


public class AssertUtils {

    /**
     * Construct.
     */
    public static void notNull(Object obj, String varName) {
        if (obj == null) {
            throw new IllegalArgumentException("'" + varName + "' must not be null");
        }
    }

    /**
     * Construct.
     */
    public static void notBlank(String str, String varName) {
        notNull(str, varName);
        if (StringUtils.isBlank(str)) {
            throw new IllegalArgumentException("'" + varName + "' must not be blank");
        }
    }

    /**
     * Construct.
     */
    public static void isPositive(long number, String varName) {
        if (number < 0) {
            throw new IllegalArgumentException("'" + varName + "' must be positive");
        }
    }

    /**
     * Construct.
     */
    public static void isTrue(boolean condition, String varName) {
        if (!condition) {
            throw new IllegalArgumentException("'" + varName + "' must be true");
        }
    }

    /**
     * Construct.
     */
    public static void isFalse(boolean condition, String varName) {
        if (condition) {
            throw new IllegalArgumentException("'" + varName + "' must be false");
        }
    }

}
