package run.ikaros.api.infra.exception.custom;

public class CustomConvertException extends CustomException {
    public CustomConvertException(String message) {
        super(message);
    }

    public CustomConvertException(String message, Throwable cause) {
        super(message, cause);
    }
}
