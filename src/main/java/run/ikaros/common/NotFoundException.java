package run.ikaros.common;

/**
 * 表示当前主体无权访问或目标对象不存在，避免泄露其他用户对象信息。
 */
public class NotFoundException extends RuntimeException {

    /**
     * 创建不存在异常。
     *
     * @param message 面向调用方的错误说明
     */
    public NotFoundException(String message) {
        super(message);
    }
}
