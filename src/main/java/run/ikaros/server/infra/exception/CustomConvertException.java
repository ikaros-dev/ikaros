package run.ikaros.server.infra.exception;

public class CustomConvertException extends CustomException {
    public CustomConvertException(String message) {
        super(message);
    }

    public CustomConvertException(String message, Throwable cause) {
        super(message, cause);
    }
}
