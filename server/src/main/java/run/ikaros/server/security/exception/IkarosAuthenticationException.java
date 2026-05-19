package run.ikaros.server.security.exception;

import org.springframework.http.HttpStatus;

public class IkarosAuthenticationException extends RuntimeException {

    private final HttpStatus httpStatus;

    public IkarosAuthenticationException(String message) {
        this(message, HttpStatus.UNAUTHORIZED);
    }

    public IkarosAuthenticationException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public IkarosAuthenticationException(String message, Throwable cause) {
        this(message, cause, HttpStatus.UNAUTHORIZED);
    }

    public IkarosAuthenticationException(String message, Throwable cause,
                                         HttpStatus httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public IkarosAuthenticationException(String message, Throwable cause,
                                         boolean enableSuppression,
                                         boolean writableStackTrace) {
        this(message, cause, enableSuppression, writableStackTrace, HttpStatus.UNAUTHORIZED);
    }

    public IkarosAuthenticationException(String message, Throwable cause,
                                         boolean enableSuppression,
                                         boolean writableStackTrace,
                                         HttpStatus httpStatus) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
