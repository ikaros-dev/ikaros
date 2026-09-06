package run.ikaros.common;

/** 请求的单一字节范围无法满足。 */
public class InvalidRangeException extends RuntimeException {
    public InvalidRangeException(String message) {
        super(message);
    }
}
