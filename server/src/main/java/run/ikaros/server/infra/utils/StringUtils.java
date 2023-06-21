package run.ikaros.server.infra.utils;

public class StringUtils {

    /**
     * upper str first char.
     */
    public static String upperCaseFirst(String val) {
        char[] arr = val.toCharArray();
        arr[0] = Character.toUpperCase(arr[0]);
        return new String(arr);
    }

    /**
     * Construct.
     *
     * @param s 待校验的字符串
     * @return 是否为空或者空串
     */
    public static boolean isBlank(final String s) {
        if (s == null || s.isEmpty()) {
            return true;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isWhitespace(c)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    public static boolean isNotBlank(final String s) {
        return !isBlank(s);
    }

    /**
     * return % + you str + % .
     */
    public static String addLikeChar(String str) {
        return "%" + str + "%";
    }

}
