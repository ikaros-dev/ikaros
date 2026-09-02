package run.ikaros.common;

/**
 * 表示请求违反唯一性或当前状态约束。
 */
public class ConflictException extends RuntimeException {

    /**
     * 创建冲突异常。
     *
     * @param message 面向调用方的错误说明
     */
    public ConflictException(String message) {
        super(message);
    }
}
