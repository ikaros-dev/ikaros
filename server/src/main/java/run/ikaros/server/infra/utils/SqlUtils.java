package run.ikaros.server.infra.utils;

public class SqlUtils {

    /**
     * 转义所有可能的特殊字符的函数.
     */
    public static String escapeLikeSpecialChars(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("([\\\\_%\\[\\]])", "\\\\$1")
            .replace("-", "\\-")
            .replace("!", "\\!")
            .replace("'", "''")
            .replace("`", "\\`")
            .replace("\"", "\\\"")
            .replace("*", "\\*")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("<", "\\<")
            .replace(">", "\\>")
            .replace("#", "\\#")
            .replace("&", "\\&")
            .replace("|", "\\|")
            .replace("^", "\\^")
            .replace("~", "\\~")
            .replace("$", "\\$")
            .replace("?", "\\?")
            .replace("+", "\\+")
            .replace(";", "\\;")
            .replace(":", "\\:")
            .replace("@", "\\@")
            .replace("/", "\\/")
            .replace("=", "\\=")
            .replace(",", "\\,")
            .replace(".", "\\.")
            .replace(" ", "\\ ");
    }
}
