package run.ikaros.api.infra.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

public class IkarosException extends ResponseStatusException {
    public IkarosException() {
        super(HttpStatus.OK);
    }

    public IkarosException(HttpStatusCode status) {
        super(status);
    }

    public IkarosException(HttpStatusCode status, String reason) {
        super(status, reason);
    }

    public IkarosException(String reason) {
        super(HttpStatus.OK, reason);
    }

    public IkarosException(int rawStatusCode, String reason, Throwable cause) {
        super(rawStatusCode, reason, cause);
    }

    public IkarosException(String reason, Throwable cause) {
        super(HttpStatus.OK, reason, cause);
    }

    public IkarosException(HttpStatusCode status, String reason,
                           Throwable cause) {
        super(status, reason, cause);
    }

    public IkarosException(HttpStatusCode status, String reason,
                           Throwable cause, String messageDetailCode,
                           Object[] messageDetailArguments) {
        super(status, reason, cause, messageDetailCode, messageDetailArguments);
    }

    public IkarosException(String reason,
                           Throwable cause, String messageDetailCode,
                           Object[] messageDetailArguments) {
        super(HttpStatus.OK, reason, cause, messageDetailCode, messageDetailArguments);
    }
}
