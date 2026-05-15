package run.ikaros.api.custom.exception;

public class CustomConvertException extends CustomException {
    public CustomConvertException(String message) {
        super(message);
    }

    public CustomConvertException(String message, Throwable cause) {
        super(message, cause);
    }
}
