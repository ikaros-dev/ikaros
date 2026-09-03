package run.ikaros.common;

/** 表示 If-Match 前置条件与当前资源版本不匹配。 */
public class PreconditionFailedException extends RuntimeException {
    public PreconditionFailedException(String message) {
        super(message);
    }
}
