package run.ikaros.api.infra.utils;

import java.security.SecureRandom;

public class StringUtils {
    private static final String CHARACTERS
        = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_-+=<>?";
    private static final SecureRandom RANDOM = new SecureRandom();

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


    /**
     * 生成指定长度的字符串.
     *
     * @param length 字符串长度
     * @return 随机生成的字符串
     */
    public static String generateRandomStr(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("字符串长度必须大于 0");
        }

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    /**
     * 判断是否经过Base64编码.
     *
     * @param str 字符串
     * @return 是否
     */
    public static boolean isBase64Encoded(String str) {
        // 判断字符串是否符合Base64编码规则
        String regex = "^[A-Za-z0-9+/=]+$";
        return str.matches(regex) && str.length() % 4 == 0;
    }

    /**
     * user_id => userId
     * .
     */
    public static String snakeToCamel(String input) {
        if (input == null || !input.contains("_")) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        boolean nextUpperCase = false;

        // 转为小写处理，确保结果符合标准驼峰（如 USER_ID -> userId）
        String lowerInput = input.toLowerCase();

        for (int i = 0; i < lowerInput.length(); i++) {
            char c = lowerInput.charAt(i);
            if (c == '_') {
                nextUpperCase = true;
            } else {
                if (nextUpperCase) {
                    result.append(Character.toUpperCase(c));
                    nextUpperCase = false;
                } else {
                    result.append(c);
                }
            }
        }
        return result.toString();
    }
}
