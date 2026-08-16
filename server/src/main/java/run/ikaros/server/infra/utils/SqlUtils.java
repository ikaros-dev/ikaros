package run.ikaros.server.infra.utils;

import org.jspecify.annotations.Nullable;

public class SqlUtils {

    /**
     * 转义所有可能的特殊字符的函数.
     */
    public static @Nullable String escapeLikeSpecialChars(@Nullable String input) {
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
