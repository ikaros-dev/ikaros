package run.ikaros.common;

/** 表示请求违反唯一性或当前状态约束。 */
public class ConflictException extends RuntimeException {
    private final String code;

    public ConflictException(String message) {
        this("conflict", message);
    }

    public ConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
