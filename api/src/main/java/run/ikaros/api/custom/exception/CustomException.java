package run.ikaros.api.custom.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import run.ikaros.api.infra.exception.IkarosException;

public class CustomException extends IkarosException {
    public CustomException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public CustomException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }

    public CustomException(HttpStatusCode status, String reason) {
        super(status, reason);
    }

    public CustomException(int rawStatusCode, String reason, Throwable cause) {
        super(rawStatusCode, reason, cause);
    }

    public CustomException(HttpStatusCode status, String reason, Throwable cause,
                           String messageDetailCode, Object[] messageDetailArguments) {
        super(status, reason, cause, messageDetailCode, messageDetailArguments);
    }
}
