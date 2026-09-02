package run.ikaros.plugin;

/** 插件兼容性版本比较，按点分数字段比较，避免字符串字典序误判。 */
final class PluginVersionCompatibility {
    private PluginVersionCompatibility() { }

    static boolean supports(String current, String minimum, String maximum) {
        if (current == null || minimum == null || !valid(current) || !valid(minimum)) return false;
        if (compare(current, minimum) < 0) return false;
        return maximum == null || (valid(maximum) && compare(current, maximum) <= 0);
    }

    private static int compare(String left, String right) {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        int length = Math.max(leftParts.length, rightParts.length);
        for (int index = 0; index < length; index++) {
            int leftPart = index < leftParts.length ? Integer.parseInt(leftParts[index]) : 0;
            int rightPart = index < rightParts.length ? Integer.parseInt(rightParts[index]) : 0;
            if (leftPart != rightPart) return Integer.compare(leftPart, rightPart);
        }
        return 0;
    }

    private static boolean valid(String version) {
        return version.matches("\\d+(?:\\.\\d+)*");
    }
}
