package run.ikaros.server.utils;

/**
 * @author li-guohao
 */
public class StringUtils {

    /**
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
     * @return % + you str + %
     */
    public static String addLikeChar(String str) {
        return "%" + str + "%";
    }

}
