package run.ikaros.common;

/** 解析资源版本的 HTTP If-Match 标记。 */
public final class IfMatchVersion {
    private IfMatchVersion() { }

    public static long parse(String value) {
        if (value == null || value.isBlank()) {
            throw new PreconditionRequiredException("If-Match 请求头为必填项");
        }
        String normalized = value.trim();
        if ("*".equals(normalized)) {
            throw new PreconditionFailedException("不支持使用通配 If-Match 更新资源");
        }
        if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (normalized.startsWith("v:")) {
            normalized = normalized.substring(2);
        }
        try {
            long version = Long.parseLong(normalized);
            if (version < 0) {
                throw new NumberFormatException("negative");
            }
            return version;
        } catch (NumberFormatException exception) {
            throw new PreconditionFailedException("If-Match 必须是有效的资源版本 ETag");
        }
    }

    public static String etag(Long version) {
        return "\"" + (version == null ? 0 : version) + "\"";
    }
}
