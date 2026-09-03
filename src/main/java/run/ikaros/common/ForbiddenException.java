package run.ikaros.common;

/** 表示当前身份无权执行请求操作。 */
public class ForbiddenException extends RuntimeException {
    private final String code;

    public ForbiddenException(String message) {
        this("authorization.denied", message);
    }

    public ForbiddenException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() { return code; }
}
