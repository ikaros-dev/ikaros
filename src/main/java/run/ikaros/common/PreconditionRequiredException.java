package run.ikaros.common;

/** 表示条件请求缺少 If-Match 前置条件。 */
public class PreconditionRequiredException extends RuntimeException {
    public PreconditionRequiredException(String message) {
        super(message);
    }
}
